# 🚀 Quick Start - FareFlow with Docker

## 5-Minute Setup

### Step 1️⃣ : Copy Environment Template
```bash
cp .env.example .env
```

### Step 2️⃣ : Get Gmail App Password

1. Go to https://myaccount.google.com/apppasswords
2. Select "Mail" and your device
3. Copy the 16-character password

### Step 3️⃣ : Edit `.env` File

```bash
nano .env
```

Update these 2 lines:
```
MYSQL_ROOT_PASSWORD=use-a-strong-password-here
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=xxxx xxxx xxxx xxxx
```

Then save (Ctrl+O, Enter, Ctrl+X in nano)

### Step 4️⃣ : Run Setup Script

```bash
chmod +x docker-setup.sh
./docker-setup.sh
```

Then select option `1` to build and start

### Step 5️⃣ : Verify It Works

```bash
# Check status
docker-compose ps

# View logs
docker-compose logs app
```

You should see: `FareFlow Backend Started`

---

## That's It! 🎉

Your application is now running:
- 🌐 **Frontend**: http://localhost:3000 (if using frontend)
- 📱 **API**: http://localhost:8080/api
- 🗄️ **Database**: mysql://localhost:3306

---

## Test Invoice Email Feature

Try sending an invoice:
1. Login to application
2. Go to Account Management → Invoices
3. Generate an invoice
4. Click "Send Email"
5. Check your inbox!

---

## If Something Goes Wrong

### Check Logs
```bash
docker-compose logs app
```

### Restart Services
```bash
docker-compose restart
```

### Full Reset (⚠️ Deletes Database)
```bash
docker-compose down -v
docker-compose up -d
```

### More Help
- See `DOCKER_SETUP.md` for detailed troubleshooting
- See `ENVIRONMENT_SETUP.md` for configuration details

---

## What Files Changed?

| File | Change | Why |
|------|--------|-----|
| `application.properties` | Uses env vars instead of hardcoded values | Security |
| `docker-compose.yml` | Reads from `.env` file | Flexible configuration |
| `Dockerfile` | Added env var documentation | Clarity |
| `.env.example` | NEW - Template file | Easy setup |
| `docker-setup.sh` | NEW - Interactive script | User-friendly |
| `DOCKER_SETUP.md` | NEW - Full documentation | Help reference |
| `ENVIRONMENT_SETUP.md` | NEW - Env var guide | Configuration help |

---

## Next Steps

- [ ] Complete steps 1-4 above
- [ ] Verify services are running
- [ ] Test email feature
- [ ] Read detailed docs if needed

---

## Files to Remember

📌 **Important:**
- ✅ `.env` - Your actual credentials (NEVER commit to Git!)
- ✅ `.env.example` - Template file (safe to share)
- ✅ `.gitignore` - Already protects `.env` file

---

Need more details? Read:
- `DOCKER_SETUP.md` - Complete Docker guide
- `ENVIRONMENT_SETUP.md` - Environment variables reference
