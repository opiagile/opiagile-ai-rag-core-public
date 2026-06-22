#!/usr/bin/env python3
import json
import os
import pathlib
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

ROOT = pathlib.Path(__file__).resolve().parents[1]
API_BASE_URL = os.environ.get('API_BASE_URL', 'http://localhost:8080').rstrip('/')
SAMPLE_FILE = ROOT / 'samples' / 'clinica' / 'faq.txt'


def fail(message):
    print(f'[falha] {message}', file=sys.stderr)
    raise SystemExit(1)


def ok(message):
    print(f'[ok] {message}')


def request_json(method, path, payload=None, timeout=15):
    data = None
    headers = {'Accept': 'application/json'}
    if payload is not None:
        data = json.dumps(payload, ensure_ascii=False).encode('utf-8')
        headers['Content-Type'] = 'application/json'
    request = urllib.request.Request(
        API_BASE_URL + path,
        data=data,
        headers=headers,
        method=method,
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            body = response.read().decode('utf-8')
            return response.status, json.loads(body) if body else None
    except urllib.error.HTTPError as exc:
        body = exc.read().decode('utf-8', errors='replace')
        fail(f'{method} {path} retornou HTTP {exc.code}: {body}')
    except urllib.error.URLError as exc:
        fail(f'{method} {path} não conectou em {API_BASE_URL}: {exc}')


def upload_file(path):
    boundary = '----opiagile-e2e-' + uuid.uuid4().hex
    head = (
        f'--{boundary}\r\n'
        f'Content-Disposition: form-data; name="file"; filename="{path.name}"\r\n'
        'Content-Type: text/plain\r\n\r\n'
    ).encode('utf-8')
    tail = f'\r\n--{boundary}--\r\n'.encode('utf-8')
    data = head + path.read_bytes() + tail
    request = urllib.request.Request(
        API_BASE_URL + '/api/documents/upload',
        data=data,
        headers={
            'Accept': 'application/json',
            'Content-Type': f'multipart/form-data; boundary={boundary}',
        },
        method='POST',
    )
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return response.status, json.loads(response.read().decode('utf-8'))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode('utf-8', errors='replace')
        fail(f'upload retornou HTTP {exc.code}: {body}')
    except urllib.error.URLError as exc:
        fail(f'upload não conectou em {API_BASE_URL}: {exc}')


def assert_field(data, field):
    if field not in data or data[field] in (None, '', []):
        fail(f'campo obrigatório ausente ou vazio: {field}. Resposta: {data}')
    return data[field]


def main():
    print(f'E2E Opiagile contra {API_BASE_URL}')

    status, health = request_json('GET', '/actuator/health')
    if status != 200 or health.get('status') != 'UP':
        fail(f'health inesperado: {health}')
    ok('health da API está UP')

    status, version = request_json('GET', '/api/version')
    if status != 200 or version.get('appName') != 'opiagile-ai-rag-core':
        fail(f'version inesperado: {version}')
    ok(f"version respondeu {version.get('version')}")

    if not SAMPLE_FILE.exists():
        fail(f'sample não encontrado: {SAMPLE_FILE}')
    status, upload = upload_file(SAMPLE_FILE)
    document_id = assert_field(upload, 'documentId')
    if upload.get('chunkCount', 0) < 1:
        fail(f'upload não gerou chunks: {upload}')
    ok(f'documento enviado com {upload.get("chunkCount")} chunks')

    status, documents = request_json('GET', '/api/documents')
    if not any(doc.get('id') == document_id for doc in documents):
        fail('documento enviado não apareceu na listagem')
    ok('listagem de documentos contém o upload do E2E')

    status, detail = request_json('GET', f'/api/documents/{document_id}')
    if detail.get('id') != document_id:
        fail(f'detalhe do documento inesperado: {detail}')
    ok('detalhe do documento respondeu corretamente')

    status, chunks = request_json('GET', f'/api/documents/{document_id}/chunks')
    if len(chunks) < 1:
        fail('chunks do documento não foram retornados')
    ok('chunks do documento foram retornados')

    contact = '+551199' + str(int(time.time()))[-6:]
    status, chat = request_json('POST', '/api/chat', {
        'message': 'Meu nome é Teste Opiagile, meu telefone é ' + contact + ' e quero agendar uma consulta no sábado. Vocês atendem aos sábados?',
        'channel': 'WEB',
        'contactId': 'e2e-web-' + uuid.uuid4().hex[:8],
    })
    conversation_id = assert_field(chat, 'conversationId')
    if not chat.get('sources'):
        fail(f'chat RAG não retornou fontes: {chat}')
    if chat.get('intent') not in ('AGENDAR', 'DUVIDA_FAQ'):
        fail(f'intenção inesperada no chat: {chat.get("intent")}')
    ok(f'chat RAG respondeu com fontes e intenção {chat.get("intent")}')

    status, follow_up = request_json('POST', '/api/chat', {
        'conversationId': conversation_id,
        'message': 'Pode considerar o mesmo telefone e me lembrar se preciso levar documentos?',
        'channel': 'WEB',
        'contactId': 'e2e-web-contexto',
    })
    if follow_up.get('conversationId') != conversation_id:
        fail('memória não manteve o conversationId')
    ok('segunda mensagem reutilizou a conversa existente')

    status, messages = request_json('GET', f'/api/conversations/{conversation_id}/messages')
    if len(messages) < 4:
        fail(f'histórico deveria ter pelo menos 4 mensagens: {messages}')
    ok('histórico de conversa foi persistido')

    status, summary = request_json('GET', f'/api/conversations/{conversation_id}/summary')
    if summary.get('conversationId') != conversation_id:
        fail(f'resumo inesperado: {summary}')
    ok('resumo da conversa foi consultado')

    status, handoff_chat = request_json('POST', '/api/chat', {
        'conversationId': conversation_id,
        'message': 'Quero falar com um atendente humano agora, por favor.',
        'channel': 'WEB',
        'contactId': 'e2e-web-handoff',
    })
    if not handoff_chat.get('handoffRequired'):
        fail(f'pedido de humano não gerou handoffRequired: {handoff_chat}')
    ok('pedido de humano acionou handoff')

    status, handoffs = request_json('GET', '/api/handoffs')
    related = [item for item in handoffs if item.get('conversationId') == conversation_id]
    if not related:
        fail('handoff da conversa não apareceu na listagem')
    handoff_id = related[0]['id']
    ok('handoff apareceu na listagem')

    status, handoff_detail = request_json('GET', f'/api/handoffs/{handoff_id}')
    if handoff_detail.get('id') != handoff_id:
        fail(f'detalhe de handoff inesperado: {handoff_detail}')
    ok('detalhe do handoff respondeu corretamente')

    status, handoff_updated = request_json('PATCH', f'/api/handoffs/{handoff_id}/status', {'status': 'IN_PROGRESS'})
    if handoff_updated.get('status') != 'IN_PROGRESS':
        fail(f'update de handoff não aplicou status: {handoff_updated}')
    ok('status do handoff foi atualizado')

    status, trace = request_json('GET', f'/api/observability/conversations/{conversation_id}/trace')
    if not trace.get('messages') or not trace.get('retrievals'):
        fail(f'trace sem mensagens ou retrievals: {trace}')
    ok('observabilidade retornou mensagens e recuperações')

    print('\nE2E concluído com sucesso.')


if __name__ == '__main__':
    main()
