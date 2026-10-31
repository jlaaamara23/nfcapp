# אתר עובדים (זמני) — Vercel

אתר מובייל להתחברות, כניסה ויציאה עד שאישור Google Play יגיע.
הוא מדבר עם אותו API של האפליקציה: `https://api.sreamline.tech`.

NFC לא נדרש כאן. העובד ממלא קוד תחנה (למשל `GATE-01`) כמו בתג.

## העלאה ל-Vercel

1. היכנסו ל-[vercel.com](https://vercel.com) עם GitHub.
2. **Add New → Project** → בחרו את הריפו `nfcapp`.
3. **Root Directory:** `employee-web`
4. Environment Variable:
   - Name: `VITE_API_BASE_URL`
   - Value: `https://api.sreamline.tech`
5. Deploy.

אחרי העלאה, העתיקו את הכתובת (למשל `https://nfcapp-xxx.vercel.app`).

## CORS בשרת (חובה)

ב-VPS, בקובץ `/opt/nfcapp/.env` הוסיפו את כתובת Vercel:

```env
APP_CORS_ALLOWED_ORIGINS=https://admin.sreamline.tech,https://*.vercel.app
```

ואז:

```bash
cd /opt/nfcapp
docker compose up -d
```

בלי זה האתר ב-Vercel לא יצליח להתחבר ל-API.

## שימוש

העובד פותח את קישור Vercel בטלפון, מתחבר עם חשבון **עובד** שנוצר בלוח הניהול, ממלא קוד תחנה, ולוחץ כניסה / יציאה.
