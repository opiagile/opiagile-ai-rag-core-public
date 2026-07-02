WITH contexto AS (
    SELECT t.id AS tenant_id, w.id AS workspace_id, w.slug AS workspace_slug
    FROM tenants t
    JOIN workspaces w ON w.tenant_id = t.id
    WHERE t.slug = 'demo'
      AND w.slug IN ('clinica-demo', 'atendimento-demo', 'locacao-demo')
),
documentos AS (
    INSERT INTO documents (tenant_id, workspace_id, filename, content_type, source_type, status)
    SELECT tenant_id,
           workspace_id,
           CASE workspace_slug
               WHEN 'clinica-demo' THEN 'clinica-demo-faq.md'
               WHEN 'atendimento-demo' THEN 'atendimento-demo-faq.md'
               WHEN 'locacao-demo' THEN 'locacao-demo-faq.md'
           END,
           'text/markdown',
           'SEED',
           'INDEXED'
    FROM contexto
    WHERE NOT EXISTS (
        SELECT 1
        FROM documents d
        WHERE d.tenant_id = contexto.tenant_id
          AND d.workspace_id = contexto.workspace_id
          AND d.filename = CASE contexto.workspace_slug
               WHEN 'clinica-demo' THEN 'clinica-demo-faq.md'
               WHEN 'atendimento-demo' THEN 'atendimento-demo-faq.md'
               WHEN 'locacao-demo' THEN 'locacao-demo-faq.md'
          END
    )
    RETURNING id, workspace_id, filename
),
documentos_existentes AS (
    SELECT d.id, d.workspace_id, d.filename
    FROM documents d
    JOIN contexto c ON c.tenant_id = d.tenant_id AND c.workspace_id = d.workspace_id
    WHERE d.filename IN ('clinica-demo-faq.md', 'atendimento-demo-faq.md', 'locacao-demo-faq.md')
),
base AS (
    SELECT id, workspace_id, filename FROM documentos
    UNION
    SELECT id, workspace_id, filename FROM documentos_existentes
)
INSERT INTO document_chunks (document_id, chunk_index, content, metadata)
SELECT b.id,
       chunk.chunk_index,
       chunk.content,
       jsonb_build_object('source', 'demo-rag-seed', 'workspace', chunk.workspace_slug, 'topic', chunk.topic, 'language', chunk.language)
FROM base b
JOIN contexto c ON c.workspace_id = b.workspace_id
JOIN (
    VALUES
        ('clinica-demo', 0, 'horario-atendimento', 'pt', 'A Clinica Demo atende de segunda a sexta, das 8h as 18h. Aos sabados, atende das 8h as 12h somente com horario agendado. Domingos e feriados nao possuem atendimento. Perguntas sobre saturday, sabados, opening hours e horario devem usar esta regra.'),
        ('clinica-demo', 1, 'documentos-consulta', 'pt', 'Para consulta na Clinica Demo, o paciente deve levar documento com foto, carteirinha do convenio quando houver, exames recentes e pedido medico se aplicavel. Se nao tiver a carteirinha, pode informar o numero do convenio ou apresentar documento com foto no atendimento.'),
        ('clinica-demo', 2, 'agendamento', 'pt', 'Para solicitar agendamento, coletar nome completo, telefone, tipo de consulta, modalidade presencial ou online e preferencia de data ou periodo. A IA nao confirma horario real, nao promete vaga e nao deve dizer que o agendamento foi concluido; deve encaminhar para confirmacao da equipe.'),
        ('clinica-demo', 3, 'remarcacao-cancelamento', 'pt', 'Consultas podem ser remarcadas com pelo menos 24 horas de antecedencia. Cancelamentos devem ser solicitados com pelo menos 12 horas de antecedencia. Cancelamentos recorrentes podem exigir confirmacao manual da equipe.'),
        ('clinica-demo', 4, 'handoff-urgencia', 'pt', 'A conversa deve ser encaminhada para uma pessoa quando o paciente pedir atendente humano, relatar reclamacao, mencionar urgencia medica, pedir informacao fora do FAQ ou quando a IA nao tiver resposta segura. Em caso de urgencia medica, orientar atendimento de emergencia imediatamente.'),
        ('clinica-demo', 5, 'clinic-english', 'en', 'Clinic Demo opens Monday to Friday from 8 AM to 6 PM. On Saturdays, it opens from 8 AM to noon by appointment only. For an appointment, collect full name, phone, consultation type, in-person or online preference, and preferred date or period. The assistant must not confirm a real time slot.'),
        ('clinica-demo', 6, 'clinic-documents-english', 'en', 'For a clinic appointment, the patient should bring a photo ID, insurance card when available, recent exams and a medical request when applicable. If the insurance card is missing, the patient may provide the insurance number or show a photo ID at reception.'),
        ('clinica-demo', 7, 'clinica-espanol', 'es', 'La Clinica Demo atiende de lunes a viernes de 8 a 18 horas. Los sabados atiende de 8 a 12 solo con cita agendada. Para una cita, solicitar nombre completo, telefono, tipo de consulta, modalidad presencial u online y fecha o periodo preferido.'),

        ('atendimento-demo', 0, 'horario-suporte', 'pt', 'O Atendimento Demo funciona de segunda a sexta, das 8h as 19h. Chamados criticos de clientes com contrato premium podem ser encaminhados para plantao. Chamados comuns recebem primeira resposta em ate 8 horas uteis. Chamados criticos recebem primeira resposta em ate 2 horas uteis.'),
        ('atendimento-demo', 1, 'abertura-chamado', 'pt', 'Para abrir um chamado de suporte, informar nome, email, empresa, produto afetado, descricao objetiva do problema, horario de ocorrencia, impacto no negocio e prints ou evidencias quando disponiveis. Esse conjunto reduz retrabalho e acelera a triagem.'),
        ('atendimento-demo', 2, 'senha-acesso', 'pt', 'Para redefinir senha, o usuario deve usar a opcao Esqueci minha senha no portal. Se nao tiver acesso ao email cadastrado, se houver suspeita de fraude ou se a conta estiver bloqueada por seguranca, o caso deve ser encaminhado para atendimento humano.'),
        ('atendimento-demo', 3, 'sla-prioridade', 'pt', 'SLA do Atendimento Demo: incidente critico com indisponibilidade total tem primeira resposta em ate 2 horas uteis; solicitacao comum tem primeira resposta em ate 8 horas uteis; duvidas administrativas podem seguir o proximo dia util. Prioridade depende de impacto, urgencia e numero de usuarios afetados.'),
        ('atendimento-demo', 4, 'handoff-suporte', 'pt', 'Encaminhar para humano quando houver indisponibilidade total, problema financeiro, reclamacao, falha recorrente, suspeita de acesso indevido, necessidade de especialista ou pedido explicito por atendimento humano. A IA deve resumir contexto e dados ja coletados.'),
        ('atendimento-demo', 5, 'support-english', 'en', 'Support Demo is available Monday to Friday from 8 AM to 7 PM. Standard tickets receive a first response within 8 business hours. Critical incidents receive a first response within 2 business hours. Critical issues include total outage, recurring failure or high business impact.'),
        ('atendimento-demo', 6, 'password-english', 'en', 'To reset a password, the user should use the Forgot password option in the portal. If the user cannot access the registered email, reports a security concern or has a locked account, the assistant must route the case to a human support analyst.'),
        ('atendimento-demo', 7, 'soporte-espanol', 'es', 'Para abrir un ticket, informar nombre, email, empresa, producto afectado, descripcion del problema, horario del incidente, impacto en el negocio y evidencias. Los incidentes criticos reciben primera respuesta en hasta 2 horas habiles.'),

        ('locacao-demo', 0, 'horario-visitas', 'pt', 'A Locacao Demo atende de segunda a sexta, das 9h as 18h, e aos sabados das 9h as 13h para visitas agendadas. Visitas a imoveis devem ser agendadas com pelo menos 24 horas de antecedencia.'),
        ('locacao-demo', 1, 'documentos-locacao', 'pt', 'Para iniciar uma locacao, podem ser solicitados documento com foto, comprovante de renda, comprovante de residencia, ficha cadastral preenchida e dados para analise de credito. Fiador, seguro fianca ou caucao dependem da regra do imovel e da analise cadastral.'),
        ('locacao-demo', 2, 'interesse-imovel', 'pt', 'Para qualificar interesse em imovel, coletar se a pessoa deseja alugar, comprar ou anunciar, cidade, bairro, faixa de preco, quantidade de quartos, necessidade de vaga, data desejada para mudanca e melhor canal de contato.'),
        ('locacao-demo', 3, 'agendamento-visita', 'pt', 'Para agendar visita, coletar nome, telefone, imovel de interesse, melhor data ou periodo e confirmar que a visita depende da disponibilidade do corretor e do proprietario. A IA nao deve confirmar visita real sem retorno da equipe.'),
        ('locacao-demo', 4, 'cancelamento-locacao', 'pt', 'Pedido de cancelamento, rescisao ou encerramento de contrato deve ser encaminhado para humano. A equipe valida prazo contratual, multa, vistoria, entrega de chaves, encargos pendentes e regras previstas no contrato de locacao.'),
        ('locacao-demo', 5, 'rental-english', 'en', 'For rental, the applicant may need a photo ID, proof of income, proof of address, completed registration form and credit analysis data. Guarantor, insurance bond or deposit depend on the property rules and the background analysis.'),
        ('locacao-demo', 6, 'visit-english', 'en', 'Property visits must be scheduled at least 24 hours in advance. To schedule a visit, collect name, phone, property of interest and preferred date or period. The assistant must not confirm a real visit before the brokerage team validates availability.'),
        ('locacao-demo', 7, 'alquiler-espanol', 'es', 'Para alquilar un inmueble, pueden solicitarse documento con foto, comprobante de ingresos, comprobante de residencia, ficha de registro y datos para analisis de credito. Cancelacion o rescision contractual debe ser encaminada a una persona.')
) AS chunk(workspace_slug, chunk_index, topic, language, content) ON chunk.workspace_slug = c.workspace_slug
ON CONFLICT DO NOTHING;
