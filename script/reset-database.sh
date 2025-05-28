#!/bin/bash
echo "🗑️  Resetando banco de dados..."
read -p "Tem certeza que deseja resetar o banco? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker-compose down postgres
    docker volume rm $(docker volume ls -q | grep parking)
    docker-compose up -d postgres
    echo "✅ Banco resetado!"
else
    echo "❌ Operação cancelada"
fi
