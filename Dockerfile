# Usa imagem oficial do Java
FROM openjdk:17-jdk-slim

# Cria diretório de trabalho no container
WORKDIR /app

# Copia todos os arquivos do projeto para o container
COPY . .

# Compila o servidor
RUN javac Backend/BinaryDecoderServer.java

# Comando para iniciar o servidor
CMD ["java", "Backend.BinaryDecoderServer"]
