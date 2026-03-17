# 로컬 DB 실행

## 기본 파일
- 예시 환경변수: `.env.example`
- 실제 로컬 환경변수: `.env`
- Docker Compose: `docker-compose.yml`

## 실행
```bash
docker compose up -d db
```

## 상태 확인
```bash
docker compose ps
docker compose logs -f db
```

## 중지
```bash
docker compose stop db
```

## 완전 삭제
```bash
docker compose down -v
```

## 백엔드 실행
기본 `.env` 예시 값을 그대로 쓰는 경우에는 `application.yml` 기본값과 맞물리므로 DB 컨테이너를 띄운 뒤 바로 백엔드를 실행하면 된다.

```bash
./mvnw spring-boot:run
```

만약 `.env`에서 포트나 계정을 바꿨다면, 같은 값을 환경변수로 넘겨 실행한다.

```bash
DB_URL='jdbc:mysql://localhost:3306/partition_mate?serverTimezone=Asia/Seoul&characterEncoding=UTF-8' \
DB_USERNAME='root' \
DB_PASSWORD='qwe123' \
./mvnw spring-boot:run
```

## 참고
- 앱 시작 시 `src/main/resources/seeds/stores.json` 기준으로 지점 데이터가 자동 적재된다.
- 기본 포트는 `3306`이고, 로컬에서 이미 사용 중이면 `.env`의 `MYSQL_PORT`와 `DB_URL`을 같이 바꿔야 한다.
