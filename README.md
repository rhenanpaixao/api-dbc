# API DBC — Consulta Tabela FIPE

Microsserviço REST em Java 17 + frontend Angular 18 que consulta a [Tabela FIPE](https://fipe.parallelum.com.br) e retorna o histórico de preços de um veículo ao longo dos anos de fabricação, incluindo a variação absoluta e percentual entre cada ano consecutivo.

---

## Stack

| Camada      | Tecnologia                                  |
|-------------|---------------------------------------------|
| Backend     | Java 17 · Spring Boot 3.5 · Spring MVC      |
| HTTP Client | `RestClient` (Spring 6.1)                   |
| Docs        | Springdoc OpenAPI 2 (Swagger UI)            |
| Build       | Maven                                       |
| Frontend    | Angular 18 · Angular Material · SCSS        |
| Formulários | Reactive Forms                              |
| Testes      | JUnit 5 · Mockito · MockMvc · AssertJ       |

---

## Arquitetura

```
┌─────────────────────────────────────────────────────┐
│  Angular 18 (frontend :4200)                        │
│  FipeComponent → FipeService → HttpClient           │
└───────────────────┬─────────────────────────────────┘
                    │ HTTP
┌───────────────────▼─────────────────────────────────┐
│  Spring Boot (backend :8080)                        │
│                                                     │
│  BrandController   /api/v1/brands                   │
│  VehicleController /api/v1/vehicles/{b}/{m}         │
│          │                                          │
│  VehicleService  ← regras de negócio                │
│          │                                          │
│  FipeClient  → RestClient                           │
└───────────────────┬─────────────────────────────────┘
                    │ HTTPS
          https://fipe.parallelum.com.br/api/v2
```

---

## Pré-requisitos

- Java 17+
- Maven 3.9+ (ou usar o `mvnw` incluído)
- Node.js 20+ · npm 10+

---

## Como executar

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

O servidor sobe em `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm start
```

O app abre em `http://localhost:4200`.

---

## Endpoints

### `GET /api/v1/brands`

Lista todas as marcas disponíveis na tabela FIPE.

| Parâmetro   | Tipo  | Default | Valores aceitos                   |
|-------------|-------|---------|-----------------------------------|
| vehicleType | query | `cars`  | `cars` · `motorcycles` · `trucks` |

**Exemplo:**
```
GET /api/v1/brands?vehicleType=cars
```

---

### `GET /api/v1/brands/{brandId}/models`

Lista todos os modelos de uma marca.

| Parâmetro   | Tipo  | Obrigatório |
|-------------|-------|-------------|
| brandId     | path  | sim         |
| vehicleType | query | não         |

**Exemplo:**
```
GET /api/v1/brands/59/models?vehicleType=cars
```

---

### `GET /api/v1/vehicles/{brandId}/{modelId}`

Retorna o histórico de preços FIPE do veículo, ordenado do ano mais recente para o mais antigo, com variação absoluta e percentual em relação ao ano imediatamente anterior.

| Parâmetro   | Tipo  | Obrigatório | Restrição |
|-------------|-------|-------------|-----------|
| brandId     | path  | sim         | ≥ 1       |
| modelId     | path  | sim         | ≥ 1       |
| vehicleType | query | não         | —         |

**Exemplo de requisição:**
```
GET /api/v1/vehicles/59/5940?vehicleType=cars
```

**Exemplo de resposta:**
```json
{
  "brand": "VW - VolksWagen",
  "model": "Gol 1.0",
  "fuel": "Gasolina",
  "priceHistory": [
    {
      "year": 2013,
      "price": 25000.00,
      "priceFormatted": "R$ 25.000,00",
      "priceDifference": 2500.00,
      "priceDifferenceFormatted": "R$ 2.500,00",
      "changePercentage": 11.11,
      "changePercentageFormatted": "11,11%",
      "comparedToYear": 2011
    },
    {
      "year": 2011,
      "price": 22500.00,
      "priceFormatted": "R$ 22.500,00"
    }
  ]
}
```

> O ano mais antigo não possui campos de variação (`priceDifference`, `changePercentage`, `comparedToYear`). Esses campos são omitidos do JSON (`@JsonInclude(NON_NULL)`).

---

## Códigos de resposta

| Status | Situação                                   |
|--------|--------------------------------------------|
| `200`  | Sucesso                                    |
| `400`  | Parâmetro inválido (ex.: brandId = 0)      |
| `404`  | Marca ou modelo não encontrado na API FIPE |
| `502`  | Erro ao se comunicar com a API FIPE        |
| `500`  | Erro interno inesperado                    |

Todos os erros seguem o contrato:
```json
{
  "status": 404,
  "message": "Recurso não encontrado na API FIPE.",
  "timestamp": "2026-05-23T14:30:00"
}
```

---

## Documentação interativa

Com o backend rodando, acesse:

```
http://localhost:8080/swagger-ui.html
```

---

## Testes

### Executar testes unitários e de integração

```bash
cd backend
./mvnw test
```

A suíte cobre:

| Classe de teste          | Tipo            | O que verifica                                                         |
|--------------------------|-----------------|------------------------------------------------------------------------|
| `VehicleServiceTest`     | Unit (Mockito)  | Cálculo de variação, deduplicação de anos, ordenação, formatação pt-BR |
| `VehicleControllerTest`  | Slice (MockMvc) | HTTP 200/400/404/502/500, `@JsonInclude(NON_NULL)`, validação `@Min`   |

---

## Postman

Importe a coleção `postman/FIPE-API.postman_collection.json` no Postman.

**Fluxo recomendado para executar como Runner:**

1. **Listar Marcas** — o teste salva automaticamente o primeiro `code` em `{{brandId}}`
2. **Listar Modelos** — salva automaticamente o primeiro `code` em `{{modelId}}`
3. **Consultar Histórico** — valida estrutura, ordenação, variações e formatação

Cada request possui assertions `pm.test(...)` que verificam:
- Status HTTP correto
- Estrutura do JSON (campos obrigatórios presentes)
- Anos em ordem decrescente
- Campos de variação ausentes no ano mais antigo
- Formatação monetária em pt-BR
- Tempo de resposta < 5 s

**Variáveis da coleção:**

| Variável      | Default                        |
|---------------|--------------------------------|
| `baseUrl`     | `http://localhost:8080/api/v1` |
| `vehicleType` | `cars`                         |
| `brandId`     | `59` (VW - VolksWagen)         |
| `modelId`     | `5940`                         |

---

## Decisões técnicas relevantes

### Deduplicação de anos por combustível
A API FIPE retorna o mesmo ano com múltiplos códigos quando o veículo possui variantes de combustível (ex.: `2022-1` Gasolina, `2022-2` Álcool). O serviço agrupa por ano de fabricação e mantém apenas o primeiro combustível, evitando entradas duplicadas na resposta.

### `BigDecimal` para valores monetários
Todos os cálculos de preço e percentual utilizam `BigDecimal` com `RoundingMode.HALF_UP`, garantindo precisão financeira sem erros de ponto flutuante.

### Tratamento centralizado de exceções
`GlobalExceptionHandler` (`@RestControllerAdvice`) captura todas as exceções e retorna respostas padronizadas. Nenhum stacktrace é exposto ao cliente.

