# Preparação para produção

Este repositório fornece modelos de serviço, proxy HTTPS e backup para instalação em Linux. Eles precisam ser ajustados ao domínio, distribuição e diretórios do servidor antes da ativação. Não publique o serviço usando o domínio de exemplo.

## Componentes e portas

- PostgreSQL: escutar apenas localmente, se estiver no mesmo host. Não exponha `5432` à internet.
- Spring Boot: o padrão agora escuta em `127.0.0.1:8080`.
- Nginx: atende a porta `443`, termina TLS e encaminha `/api` para o backend.
- Frontend: arquivos estáticos gerados por `npm run build`.

## Preparar servidor

No servidor, instale JDK 21, PostgreSQL 17, Nginx, Certbot e `age` pelos repositórios oficiais da distribuição. Node.js 22 é necessário na máquina de build do frontend. Crie o usuário de sistema `nucleo` sem login interativo e prepare os diretórios:

```bash
sudo useradd --system --home-dir /opt/nucleo --shell /usr/sbin/nologin nucleo
sudo install -d -o root -g nucleo -m 0750 /opt/nucleo
sudo install -d -o root -g root -m 0755 /var/www/nucleo
sudo install -d -o root -g root -m 0750 /etc/nucleo
```

Crie um banco e um usuário PostgreSQL dedicados. Use uma senha aleatória exclusiva e restrinja `pg_hba.conf` a conexões locais. Aplique as migrações pela inicialização da aplicação.

## Build e instalação

Na máquina de build, execute:

```bash
cd backend
./mvnw clean package
cd ../frontend
npm ci
npm run build
```

Copie o JAR para `/opt/nucleo/suporte.jar` com proprietário `root:nucleo` e modo `0640`. Copie o conteúdo de `frontend/dist/` para `/var/www/nucleo/`. Publique `deploy/systemd/nucleo.service` em `/etc/systemd/system/nucleo.service`.

Crie `/etc/nucleo/nucleo.env` com proprietário `root`, grupo `nucleo` e modo `0640`:

```ini
SERVER_ADDRESS=127.0.0.1
PORT=8080
DB_URL=jdbc:postgresql://127.0.0.1:5432/suporte
DB_USERNAME=nucleo_app
DB_PASSWORD=senha-aleatoria-do-banco
BOOTSTRAP_ADMIN_EMAIL=admin@empresa.exemplo
BOOTSTRAP_ADMIN_PASSWORD=senha-inicial-aleatoria-com-12-caracteres-ou-mais
COOKIE_SECURE=true
```

Use um gerenciador de segredos da organização se disponível. Não coloque esse arquivo no repositório, em imagens, logs ou argumentos de linha de comando. Após criar o primeiro administrador e trocar sua senha, remova `BOOTSTRAP_ADMIN_PASSWORD` do ambiente do serviço.

Ative o serviço:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now nucleo
sudo systemctl status nucleo
curl http://127.0.0.1:8080/actuator/health
```

## HTTPS e proxy

Antes de instalar `deploy/nginx/suporte.conf`, substitua `suporte.exemplo.com` pelo domínio real. Aponte o DNS para o servidor e obtenha o certificado com desafio DNS ou modo standalone do Certbot enquanto a porta 80 estiver livre; depois confira os caminhos dos arquivos no Nginx. Copie o arquivo para `/etc/nginx/conf.d/nucleo.conf`. Configure o firewall para permitir somente SSH administrativo e HTTPS público. Valide a sintaxe com `sudo nginx -t`, recarregue o Nginx e confira redirecionamento HTTP para HTTPS, cookie `Secure`, CSRF, login/logout e chamadas `/api`.

Renove certificados com o timer/serviço do Certbot da distribuição. Monitore a validade e teste a renovação periodicamente. Depois de confirmar que todo o domínio funciona por HTTPS, habilite HSTS no Nginx e teste novamente.

## Segredos do PostgreSQL

Os scripts `deploy/backup-postgres.sh` e `deploy/restore-postgres.sh` usam `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` e `PGPASSFILE`. O arquivo apontado por `PGPASSFILE` deve usar formato `.pgpass`, pertencer à conta que executa o script e ter modo `0600`; assim a senha não aparece na linha de comando.

Exemplo de configuração protegida em `/etc/nucleo/backup.env`:

```ini
PGHOST=127.0.0.1
PGPORT=5432
PGDATABASE=suporte
PGUSER=nucleo_app
PGPASSFILE=/etc/nucleo/pgpass
BACKUP_DIR=/var/backups/nucleo
AGE_RECIPIENT=age1chavepublica...
```

Gere um par de chaves `age`, guarde a chave privada fora do servidor e informe apenas a chave pública como `AGE_RECIPIENT`. O backup é criptografado durante a gravação; a restauração recebe a chave privada em `AGE_IDENTITY` e descriptografa em fluxo, sem criar cópia temporária aberta. O arquivo `.pgpass` deve pertencer a `nucleo`, modo `0600`, com uma linha no formato `127.0.0.1:5432:suporte:nucleo_app:SENHA_DO_BANCO`.

Instale os scripts e arquivos do agendador em `/opt/nucleo/deploy` e crie o destino de backup:

```bash
sudo install -d -o root -g nucleo -m 0750 /opt/nucleo/deploy
sudo install -d -o nucleo -g nucleo -m 0700 /var/backups/nucleo
sudo install -o root -g nucleo -m 0750 deploy/backup-postgres.sh /opt/nucleo/deploy/
sudo install -o root -g nucleo -m 0750 deploy/restore-postgres.sh /opt/nucleo/deploy/
sudo install -o root -g root -m 0644 deploy/systemd/nucleo-backup.service /etc/systemd/system/
sudo install -o root -g root -m 0644 deploy/systemd/nucleo-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now nucleo-backup.timer
systemctl list-timers nucleo-backup.timer
```

Crie `/etc/nucleo/backup.env` com proprietário `root:nucleo`, modo `0640`; configure `AGE_RECIPIENT` ali. Crie `/etc/nucleo/pgpass` com proprietário `nucleo:nucleo` e modo `0600`. Restrinja permissões e guarde uma cópia dos dumps criptografados em armazenamento externo. Defina retenção compatível com a política da organização. Um backup só é confiável depois de restaurado com sucesso.

## Exercício de restauração

Faça este exercício em um host ou banco de homologação isolado:

1. Crie um banco PostgreSQL vazio de teste e configure as variáveis de conexão para ele.
2. Execute o backup e confira se o arquivo `.dump.age` foi criado com permissões restritas.
3. Crie um chamado de referência, faça novo backup, remova o registro no banco de teste e restaure esse backup usando `deploy/restore-postgres.sh`.
4. Confirme o chamado, comentários, histórico, usuários, categorias e autenticação após a restauração.
5. Registre data, duração, tamanho, resultado e responsável. Repita após mudanças de versão ou de esquema.

O script de restauração também requer `AGE_IDENTITY`, pede o nome exato do banco como confirmação e substitui objetos existentes. Nunca faça o primeiro exercício em produção.

Para restaurar manualmente, transfira a chave privada de forma segura para um arquivo temporário legível somente pela conta `nucleo`, carregue `/etc/nucleo/backup.env`, defina `BACKUP_FILE` e `AGE_IDENTITY` e execute o script em um terminal interativo como `nucleo`. Apague o arquivo temporário da chave após o exercício.

## Homologação e liberação

Antes da produção, execute a suíte CI, percorra todos os cenários de [validação manual](07-validacao-manual.md) com contas distintas, revise logs e permissões, teste em celular e por teclado, confirme limites de acesso à rede e conclua um exercício documentado de restauração. Defina responsáveis por atualização, monitoramento, incidentes, retenção e rollback compatível com as migrações do banco.
