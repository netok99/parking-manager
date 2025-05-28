#!/bin/bash
echo "🚀 Iniciando ambiente de desenvolvimento..."
docker-compose down

echo "📦 Iniciando PostgreSQL..."
docker-compose up -d postgres
echo "⏳ Aguardando PostgreSQL..."
until docker-compose exec postgres pg_isready -U parking_user -d parking_management; do
  sleep 2
done
echo "✅ PostgreSQL pronto!"

echo "📦 Iniciando Simulador de estacionamento..."
docker-compose --profile simulator up -d garage-simulator
echo "🏁 Simulador disponível em: http://localhost:3000"

echo ""
echo "🎯 Ambiente pronto!"
echo "📊 PostgreSQL: localhost:5432"
echo "🏁 Simulador: http://localhost:3000"
echo ""
echo "Para parar tudo: ./scripts/stop-dev.sh"
