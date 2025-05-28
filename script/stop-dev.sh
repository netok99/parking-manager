#!/bin/bash
echo "🛑 Parando ambiente de desenvolvimento..."
docker-compose --profile tools --profile simulator down
echo "✅ Ambiente parado!"
