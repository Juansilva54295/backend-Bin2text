# Usa imagem do Java
FROM openjdk:17

# Cria diretório de trabalho
WORKDIR /app

# Copia os arquivos
COPY . .

# Compila
RUN javac Backend/BinaryDecoderServer.java

# Roda
CMD ["java", "Backend.BinaryDecoderServer"]
