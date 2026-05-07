FROM node:20-slim

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

# A porta será definida pelo docker-compose ou comando
EXPOSE 4015
EXPOSE 8081

CMD ["node", "index.js"]
