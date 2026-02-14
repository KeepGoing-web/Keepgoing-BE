# Blingbling-BE

## 로컬 실행 (direnv)

> `.envrc`는 개인 로컬 설정 파일이므로 **커밋하지 않습니다.**  
> 대신 템플릿 파일인 `.envrc.example`를 커밋하고, 각자 복사해서 사용합니다.

### 1) direnv 설치 (macOS)

```bash
brew install direnv
echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc
source ~/.zshrc
```

### 2) .envrc 생성 및 적용

```
cp .envrc.example .envrc
direnv allow
```

### 3) 로컬 DB 실행 (MySQL)

```
docker compose up -d
docker ps
```

Docker Desktop 대신 Colima를 사용하는 경우:

```
colima start --cpu 2 --memory 4 --disk 20
docker context use colima
docker info
```

### 4) 애플리케이션 실행 (local 프로필)

```
./gradlew bootRun
```

### 5) 종료 / 정리
- 애플리케이션 종료: 실행 중인 터미널에서 `Ctrl + C`
- MySQL 컨테이너 종료:
```
docker compose down
```

### 6) 동작 확인 (예시)
```
curl -i -H "Authorization: Bearer <ACCESS_TOKEN>" "http://localhost:8080/api/posts/me/search?keyword=spring&page=0&size=10"
```
