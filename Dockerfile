FROM openjdk:17

# Cria diretório de trabalho no container
WORKDIR /app

# Copia todos os arquivos do projeto para o container
COPY . .

# Compila todos os arquivos Java do backend
RUN javac Backend/*.java

# Comando para iniciar o servidor
CMD ["java", "Backend.BinaryDecoderServer"]
