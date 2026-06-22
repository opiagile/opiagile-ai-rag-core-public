import json
import os
import sys
import time
import urllib.request
from pathlib import Path

BASE_URL = os.environ.get('API_BASE_URL', 'http://localhost:8080')
SAMPLE_PATH = Path(os.environ.get('SAMPLE_PATH', 'samples/clinica/faq.txt'))

QUESTIONS = [
    ('sabados', 'Vocês atendem aos sábados?'),
    ('documentos', 'Quais documentos preciso levar?'),
    ('lead', 'Meu nome é João e quero agendar uma consulta.'),
    ('memoria', 'E aos sábados?'),
    ('handoff', 'Quero falar com um atendente humano.'),
    ('fora_escopo', 'Vocês fazem cirurgia de emergência?'),
]

BLOCKED_TERMS = ['chunk', 'score', 'retrieval', 'embedding', 'provider', 'prompt']


def request_json(path, payload=None, method='GET', headers=None, timeout=40):
    data = None if payload is None else json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(
        BASE_URL + path,
        data=data,
        headers=headers or {'Content-Type': 'application/json'},
        method=method,
    )
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return json.loads(response.read().decode('utf-8'))


def wait_health():
    last_error = None
    for _ in range(60):
        try:
            health = request_json('/actuator/health', headers={}, timeout=5)
            if health.get('status') == 'UP':
                return
        except Exception as exc:
            last_error = exc
        time.sleep(1)
    raise RuntimeError(f'API não ficou saudável: {last_error}')


def upload_sample():
    boundary = '----opiagilellmvalidation'
    content = SAMPLE_PATH.read_bytes()
    body = b''.join([
        f'--{boundary}\r\n'.encode(),
        f'Content-Disposition: form-data; name="file"; filename="{SAMPLE_PATH.name}"\r\n'.encode(),
        b'Content-Type: text/plain\r\n\r\n',
        content,
        b'\r\n',
        f'--{boundary}--\r\n'.encode(),
    ])
    req = urllib.request.Request(
        BASE_URL + '/api/documents/upload',
        data=body,
        headers={'Content-Type': f'multipart/form-data; boundary={boundary}'},
        method='POST',
    )
    with urllib.request.urlopen(req, timeout=40) as response:
        return json.loads(response.read().decode('utf-8'))


def chat(message, conversation_id=None):
    payload = {
        'message': message,
        'channel': 'WEB',
        'contactId': 'github-actions-v041',
    }
    if conversation_id:
        payload['conversationId'] = conversation_id
    return request_json('/api/chat', payload, method='POST')


def score_response(label, response):
    answer = response.get('answer') or ''
    answer_lower = answer.lower()
    sources = response.get('sources') or []
    mode_ok = response.get('responseMode') == 'LLM'
    provider_ok = response.get('llmProvider') == 'OPENAI'
    no_fallback = response.get('fallbackReason') in (None, '')
    no_jargon = not any(term in answer_lower for term in BLOCKED_TERMS)
    natural = len(answer.split()) >= 8 and len(answer) <= 900
    sources_ok = bool(sources) or label in ('handoff', 'fora_escopo')
    handoff_ok = response.get('handoffRequired') is True if label in ('handoff', 'fora_escopo') else True
    score = sum([mode_ok, provider_ok, no_fallback, no_jargon, natural, sources_ok, handoff_ok])
    return min(5, max(0, score - 1))


def main():
    if not os.environ.get('OPENAI_API_KEY'):
        print('OPENAI_API_KEY ausente no ambiente de validação.', file=sys.stderr)
        return 2
    wait_health()
    upload = upload_sample()
    print(f"Documento de teste indexado: {upload.get('filename')} status={upload.get('status')} chunks={upload.get('chunkCount')}")

    conversation_id = None
    results = []
    for label, question in QUESTIONS:
        response = chat(question, conversation_id)
        conversation_id = response.get('conversationId') or conversation_id
        result = {
            'label': label,
            'question': question,
            'conversationId': response.get('conversationId'),
            'responseMode': response.get('responseMode'),
            'llmProvider': response.get('llmProvider'),
            'model': response.get('model'),
            'fallbackReason': response.get('fallbackReason'),
            'handoffRequired': response.get('handoffRequired'),
            'intent': response.get('intent'),
            'leadStatus': response.get('leadStatus'),
            'sourcesCount': len(response.get('sources') or []),
            'answerPreview': (response.get('answer') or '')[:260],
            'fluencyScore': score_response(label, response),
        }
        results.append(result)

    failures = [
        r for r in results
        if r['responseMode'] != 'LLM'
        or r['llmProvider'] != 'OPENAI'
        or str(r.get('fallbackReason') or '').startswith('OPENAI_')
    ]
    low_scores = [r for r in results if r['fluencyScore'] < 4]
    output = {
        'model': results[0]['model'] if results else None,
        'conversationId': conversation_id,
        'results': results,
        'summary': {
            'total': len(results),
            'llmFailures': len(failures),
            'scoresAtLeast4': len([r for r in results if r['fluencyScore'] >= 4]),
            'minimumScore': min((r['fluencyScore'] for r in results), default=0),
        },
    }
    print(json.dumps(output, ensure_ascii=False, indent=2))
    if failures:
        print('Falha: alguma resposta não usou LLM/OpenAI ou teve fallback.', file=sys.stderr)
        return 3
    if len([r for r in results if r['fluencyScore'] >= 4]) < 4:
        print('Falha: menos de 4 respostas atingiram score mínimo 4.', file=sys.stderr)
        return 4
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
