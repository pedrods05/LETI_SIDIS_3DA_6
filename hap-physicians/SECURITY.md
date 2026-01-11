# Security Implementation

Este documento explica as **medidas de segurança** implementadas no `hap-physicians` para proteger interações entre utilizadores e serviços, garantindo conformidade com GDPR para dados sensíveis de saúde.

## Por que Segurança em Múltiplas Camadas?

Em sistemas de saúde, dados são extremamente sensíveis. Por isso, implementámos segurança em **múltiplas camadas** - se uma camada falhar, outras continuam a proteger. É como ter múltiplas fechaduras numa porta: mesmo que uma seja comprometida, outras continuam a proteger.

---

## 1. OAuth2 Resource Server

### O que é e por que é importante?

OAuth2 é um padrão usado por empresas como Google e Microsoft para autorização. No nosso sistema, o `hap-physicians` atua como **Resource Server** - recebe pedidos de clientes e verifica se têm permissão para aceder aos recursos.

### Como funciona no nosso sistema:

1. **Utilizador autentica-se** no serviço de autenticação (`hap-auth`) e recebe um "bilhete" (token JWT)
2. **Utilizador envia o bilhete** em cada pedido ao `hap-physicians` no header `Authorization: Bearer <token>`
3. **hap-physicians valida o bilhete** - verifica se é válido (não expirou, assinatura correta) e extrai informações (quem é o utilizador, que roles tem)

### Por que esta decisão?

- **Padrão da indústria:** Amplamente usado e compreendido
- **Escalabilidade:** Qualquer instância do serviço pode validar tokens sem consultar uma base de dados central
- **Separação de responsabilidades:** O `hap-auth` gere autenticação, o `hap-physicians` apenas valida tokens

**Em termos simples:** É como um segurança que verifica o bilhete de entrada. O bilhete foi emitido por outro serviço (`hap-auth`), mas o `hap-physicians` pode verificar se é válido sem precisar de ligar ao serviço de autenticação a cada pedido.

---

## 2. JWT (JSON Web Tokens)

### O que é e por que é importante?

JWT são "bilhetes de identidade" que o utilizador carrega consigo. Contêm toda a informação necessária (quem é, que permissões tem) sem precisar de consultar uma base de dados.

### Como funciona:

**Estrutura de um JWT:**
- **Header:** Tipo de token e como foi assinado
- **Payload:** Informações do utilizador (ID, email, roles/permissões)
- **Signature:** Assinatura que garante que o token não foi alterado

**Validação:**
- O serviço verifica a assinatura usando uma chave secreta partilhada
- Verifica se o token não expirou
- Extrai informações como roles e user ID

### O que está implementado:

**Claims do Token (informações incluídas):**
- User ID (identificador único)
- Email do utilizador
- Roles: ADMIN, PHYSICIAN ou PATIENT

**Propagação Automática:**
Quando o `hap-physicians` precisa de fazer uma chamada a outro serviço (ex: `hap-patients`), o token JWT é automaticamente incluído na chamada. Isto garante que o contexto de "quem está a fazer o pedido" seja mantido através de múltiplos serviços.

**Em termos simples:** É como um cartão de identificação que o utilizador carrega. Cada serviço pode verificar o cartão sem precisar de ligar a uma base de dados central para confirmar a identidade.

### Por que esta decisão?

- **Performance:** Validação é rápida (apenas verificação criptográfica)
- **Escalabilidade:** Qualquer instância pode validar tokens sem comunicação adicional
- **Simplicidade:** Não precisa de sessões ou cache distribuído

---

## 3. mTLS (Mutual TLS)

### O que é e por que é importante?

mTLS é como ter uma conversa privada onde ambas as partes se identificam com certificados. Enquanto TLS normal apenas o servidor apresenta certificado, em mTLS tanto o cliente quanto o servidor apresentam certificados válidos.

### Por que é crítico para dados de saúde?

**Encriptação de Dados Sensíveis:**
- Dados de saúde são protegidos por regulamentações rigorosas (GDPR)
- mTLS garante que todas as comunicações entre serviços sejam encriptadas, mesmo dentro da rede interna
- Previne que dados sejam interceptados mesmo se alguém conseguir acesso à rede

**Autenticação Mútua:**
- Apenas serviços com certificados válidos podem comunicar entre si
- Previne acesso não autorizado mesmo se um atacante conseguir acesso à rede interna
- Garante que apenas serviços autorizados possam aceder a dados sensíveis

### O que está implementado:

**Configuração:**
- **Keystore:** Cada serviço tem o seu próprio certificado (`hap-keystore.p12`)
- **Truststore:** Cada serviço tem uma lista de certificados de serviços confiáveis (`hap-truststore.p12`)
- Certificados armazenados em `src/main/resources/certs/`

**Como funciona na prática:**
- Quando um serviço tenta conectar a outro, ambos apresentam os seus certificados
- Apenas se ambos os certificados forem válidos (estiverem no truststore do outro), a conexão é estabelecida
- Toda a comunicação é encriptada

**Proteção de Endpoints:**
- Endpoints `/internal/**` são protegidos por mTLS
- Apenas serviços com certificados válidos podem acessá-los
- Endpoints públicos (como Swagger) permitem acesso sem certificado para facilitar desenvolvimento

**Em termos simples:** É como ter uma conversa privada onde ambas as partes mostram identificação oficial. Só se ambas as identificações forem válidas, a conversa acontece, e tudo é encriptado para que ninguém mais possa ouvir.

### Por que esta decisão?

- **Segurança adicional:** Em sistemas de saúde, qualquer camada extra de segurança é valiosa
- **Proteção de rede interna:** Mesmo dentro da rede privada, dados sensíveis devem ser encriptados
- **Autenticação de serviços:** Garante que apenas serviços autorizados possam comunicar

---

## 4. RBAC (Role-Based Access Control)

### O que é e por que é importante?

RBAC é um modelo onde cada utilizador tem uma ou mais "roles" (papéis), e cada endpoint requer roles específicas para acesso. É como ter diferentes níveis de acesso num edifício: alguns podem entrar em todas as salas, outros apenas em salas específicas.

### Como funciona:

**Roles no sistema:**
- **ADMIN:** Acesso completo a todos os recursos - pode fazer qualquer operação
- **PHYSICIAN:** Acesso a consultas, pacientes (perfis completos), notas de consultas - médicos precisam de ver informações completas para tratar pacientes
- **PATIENT:** Acesso limitado ao próprio perfil e consultas próprias - pacientes só podem ver e modificar os seus próprios dados

**Proteção de Endpoints:**
Cada endpoint é protegido com uma anotação que especifica quais roles podem acessá-lo. O sistema verifica automaticamente se o utilizador tem a role necessária antes de permitir acesso.

### O que está implementado:

**Exemplos de proteção no módulo:**

**Endpoints de Consultas:**
- Criar consulta: ADMIN, PHYSICIAN ou PATIENT podem criar
- Ver consultas: Apenas ADMIN ou PHYSICIAN (pacientes não podem ver consultas de outros)
- Cancelar consulta: Apenas ADMIN (cancelamento é uma operação sensível)
- Ver histórico de auditoria: ADMIN ou PHYSICIAN (apenas pessoal autorizado pode ver quem acedeu a dados)

**Endpoints de Auditoria:**
- Ver trilho de auditoria: ADMIN ou PHYSICIAN (apenas pessoal autorizado pode ver histórico de acessos)

**Em termos simples:** É como ter diferentes chaves para diferentes portas. Um paciente tem uma chave que só abre a porta do seu próprio quarto. Um médico tem uma chave que abre portas de consultas e perfis de pacientes. Um administrador tem uma chave mestra que abre tudo.

### Por que esta decisão?

- **Princípio do menor privilégio:** Cada utilizador tem apenas as permissões necessárias para o seu papel
- **Simplicidade:** Mais fácil de gerir do que permissões individuais para cada utilizador
- **Conformidade:** Facilita cumprir requisitos de GDPR - apenas pessoal autorizado acede a dados sensíveis

---

## 5. GDPR Compliance

### O que é GDPR e por que é importante?

GDPR (General Data Protection Regulation) é o regulamento europeu para proteção de dados pessoais. Para dados de saúde, os requisitos são ainda mais rigorosos porque são considerados "dados sensíveis".

### Requisitos Principais do GDPR:

1. **Direito de Acesso:** Utilizadores podem solicitar todos os dados que uma organização tem sobre eles
2. **Direito de Portabilidade:** Utilizadores podem solicitar que os seus dados sejam exportados
3. **Accountability:** Organizações devem poder demonstrar quem acedeu a dados e quando
4. **Proteção de Dados Sensíveis:** Dados de saúde requerem proteções adicionais

### O que está implementado:

#### Audit Trails (Trilhos de Auditoria)

**O que é:** Todas as operações sobre dados sensíveis são registadas num Event Store imutável (não pode ser alterado ou apagado).

**O que é registado:**
- **Criação de consultas:** Quando uma consulta é criada, quem criou, quando
- **Atualização de consultas:** Qualquer alteração a uma consulta é registada
- **Cancelamento de consultas:** Quando e quem cancelou
- **Adição de notas:** Quando notas são adicionadas a consultas, por quem
- **Conclusão de consultas:** Quando consultas são marcadas como concluídas

**Informações em cada evento:**
- **Timestamp:** Quando a operação ocorreu (precisão de milissegundos)
- **User ID:** Quem executou a operação
- **Correlation ID:** Permite rastrear operações relacionadas através de múltiplos serviços
- **Event Data:** Dados completos do evento (o que foi alterado, valores antigos e novos)

**Endpoint de Auditoria:**
- `GET /appointments/{id}/audit-trail` - Permite ver o histórico completo de todas as alterações a uma consulta
- Protegido: Apenas ADMIN ou PHYSICIAN podem acessar

**Por que é importante?** Permite rastrear exatamente quem acedeu a dados e quando, cumprindo requisitos de accountability do GDPR. Se houver uma investigação ou um utilizador solicitar informações sobre acessos aos seus dados, podemos fornecer um histórico completo e confiável.

**Em termos simples:** É como ter um livro de registos onde todas as ações são escritas com data, hora e quem fez. Este livro não pode ser alterado ou apagado, garantindo um histórico completo e confiável.

#### Proteção de Endpoints Sensíveis

Endpoints que acedem a dados de saúde são protegidos com múltiplas camadas:
- **mTLS:** Comunicações são encriptadas
- **RBAC:** Apenas roles autorizadas podem acessar
- **JWT Validation:** Tokens são validados em cada requisição

**Por que múltiplas camadas?** Segurança em profundidade - se uma camada falhar, outras continuam a proteger. Além disso, diferentes camadas protegem contra diferentes tipos de ameaças.

#### Encriptação em Trânsito

mTLS garante que dados sensíveis sejam encriptados durante transmissão entre serviços. Isto é um requisito do GDPR para dados sensíveis.

---

## Endpoints

### Públicos (Sem Autenticação):

Alguns endpoints são públicos por razões específicas:
- `POST /physicians/register` - Registo de médicos (precisa ser público para novos médicos se registarem)
- `/internal/**` - Endpoints internos (protegidos por mTLS - apenas serviços com certificados válidos podem acessar)
- `/swagger-ui.html` - Documentação da API (público para facilitar desenvolvimento)
- `/actuator/**` - Endpoints de monitorização (públicos para permitir monitorização do sistema)

### Protegidos:

Todos os outros endpoints requerem autenticação JWT e roles apropriadas. Isto inclui:
- Todas as operações sobre consultas (criar, ver, atualizar, cancelar)
- Acesso a perfis de pacientes
- Endpoints de auditoria
- Qualquer operação que aceda a dados sensíveis

---
