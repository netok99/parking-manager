# parking-manager

Parking manager

## Comandos para Iniciar

Dar permissão aos scripts:

```console
chmod +x scripts/*.sh
```

Iniciar ambiente:

```console
./scripts/start-dev.sh
```

Testar conexão:

```console
curl http://localhost:3000/garage
```

Ver logs do PostgreSQL:

```console
docker-compose logs -f postgres
```

Conectar no PostgreSQL:

```console
docker-compose exec postgres psql -U parking_user -d parking_management
```

## Verificação do Ambiente

Health Check do Sistema

PostgreSQL:

```console
docker-compose exec postgres pg_isready
```

Simulador:

```console
curl http://localhost:3000/garage
```

Aplicação:

```console
curl http://localhost:3003/actuator/health
```
