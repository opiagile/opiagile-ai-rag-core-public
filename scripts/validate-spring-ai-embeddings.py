import json
import os
import sys
import time
import urllib.request
from pathlib import Path

BASE_URL = os.environ.get('API_BASE_URL', 'http://localhost:8080')
SAMPLE_PATH = Path(os.environ.get('SAMPLE_PATH', 'samples/clinica/faq.txt'))


def request_json(path, payload=None, method='GET', headers=None, timeout=60):
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
    for _ in range(90):
        try:
            health = request_json('/actuator/health', headers={}, timeout=5)
            if health.get('status') == 'UP':
                return
        except Exception as exc:
            last_error = exc
        time.sleep(1)
    raise RuntimeError(f'API não ficou saudável: {last_error}')


def upload_sample():
    boundary = '----opiagilespringaiembeddings'
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
    with urllib.request.urlopen(req, timeout=90) as response:
        return json.loads(response.read().decode('utf-8'))


def chat(message):
    return request_json('/api/chat', {
        'message': message,
        'channel': 'WEB',
        'contactId': 'github-actions-spring-ai-embeddings',
    }, method='POST', timeout=90)


def fail(message, output=None, code=2):
    if output is not None:
        print(json.dumps(output, ensure_ascii=False, indent=2))
    print(f'Falha: {message}', file=sys.stderr)
    return code


def main():
    if not os.environ.get('OPENAI_API_KEY'):
        print('OPENAI_API_KEY ausente no ambiente de validação.', file=sys.stderr)
        return 2

    wait_health()
    provider_status = request_json('/api/providers/status')
    embeddings = provider_status.get('embeddings') or {}
    retrieval = provider_status.get('retrieval') or {}

    if embeddings.get('activeProvider') != 'SPRING-AI':
        return fail('provider ativo de embeddings não é SPRING-AI.', provider_status, 3)
    if embeddings.get('status') != 'OPERACIONAL':
        return fail('embeddings Spring AI não estão operacionais por configuração.', provider_status, 4)
    if retrieval.get('activeStrategy') != 'PGVECTOR_COM_FALLBACK_TEXTUAL':
        return fail('estratégia de recuperação não indica pgvector com fallback textual.', provider_status, 5)

    upload = upload_sample()
    if upload.get('embeddingProvider') != 'spring-ai':
        return fail('upload não gravou chunks com provider spring-ai.', {
            'providerStatus': provider_status,
            'upload': upload,
        }, 6)
    if int(upload.get('chunkCount') or 0) < 1:
        return fail('upload não gerou chunks.', upload, 7)

    chat_response = chat('Vocês atendem aos sábados?')
    conversation_id = chat_response.get('conversationId')
    sources = chat_response.get('sources') or []
    if not conversation_id:
        return fail('chat não retornou conversationId.', chat_response, 8)
    if not sources:
        return fail('chat não retornou fontes após ingestão com Spring AI embeddings.', chat_response, 9)

    trace = request_json(f'/api/observability/conversations/{conversation_id}/trace')
    retrievals = trace.get('retrievals') or []
    providers = [item.get('provider') for item in retrievals]
    if not any(str(provider).startswith('pgvector-spring-ai') for provider in providers):
        return fail('trace não registrou recuperação pgvector-spring-ai.', {
            'providerStatus': provider_status,
            'upload': upload,
            'chat': {
                'conversationId': conversation_id,
                'sourcesCount': len(sources),
                'responseMode': chat_response.get('responseMode'),
                'llmProvider': chat_response.get('llmProvider'),
                'fallbackReason': chat_response.get('fallbackReason'),
            },
            'retrievalProviders': providers,
        }, 10)

    output = {
        'summary': {
            'status': 'OK',
            'embeddingProvider': upload.get('embeddingProvider'),
            'chunkCount': upload.get('chunkCount'),
            'sourcesCount': len(sources),
            'retrievalProviders': providers,
        },
        'providerStatus': provider_status,
        'upload': upload,
        'chat': {
            'conversationId': conversation_id,
            'responseMode': chat_response.get('responseMode'),
            'llmProvider': chat_response.get('llmProvider'),
            'fallbackReason': chat_response.get('fallbackReason'),
            'sourcesCount': len(sources),
            'answerPreview': (chat_response.get('answer') or '')[:220],
        },
    }
    print(json.dumps(output, ensure_ascii=False, indent=2))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
