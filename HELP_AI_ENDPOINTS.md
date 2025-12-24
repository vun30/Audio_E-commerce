# AI endpoints (Railway / Local)

## Base path
Controller: `ProductAiQueryController`

- Base mapping: `/api/ai/products`

## 1) Nạp sản phẩm làm LAST_PRODUCT

### Request
- Method: `POST`
- URL: `/api/ai/products/advise`
- Query params:
  - `userId`
  - `productId` (UUID)
- Auth: `Authorization: Bearer <token>` (required)

### Example (Railway)
```
curl -X POST "https://audioe-commerce-production.up.railway.app/api/ai/products/advise?userId=GFSDFGSGVS&productId=4fdb628f-c240-4274-8458-b4797142799a" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "accept: */*"
```

## 2) Chat / tư vấn (không còn product_search)

### Request
- Method: `POST`
- URL: `/api/ai/products/search`
- JSON body:
  - `userId`
  - `question` (or `message`)

### Example (Railway)
```
curl -X POST "https://audioe-commerce-production.up.railway.app/api/ai/products/search" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"userId":"GFSDFGSGVS","question":"loa này nghe nhạc pop ổn không"}'
```

## Common 404 cause
If you see URL like:

`/api/ai/products/api/products/advise`

That means you accidentally concatenated **two base paths**. The correct endpoint is only:

`/api/ai/products/advise`

