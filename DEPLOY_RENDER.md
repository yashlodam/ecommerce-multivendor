# 🚀 Complete Beginner's Guide: Deploying ShopSphere to Render

This step-by-step guide walks you through deploying the **ShopSphere Multi-Vendor E-Commerce Platform** to **Render** using Render's Docker runtime.

---

## 📌 Table of Contents
1. [Important Warnings & Free Tier Reality](#1-important-warnings--free-tier-reality)
2. [Prerequisites](#2-prerequisites)
3. [Step 1: Push Your Code to GitHub](#step-1-push-your-code-to-github)
4. [Step 2: Create a PostgreSQL Database](#step-2-create-a-postgresql-database)
5. [Step 3: Create the Backend Web Service on Render](#step-3-create-the-backend-web-service-on-render)
6. [Step 4: Configure Environment Variables](#step-4-configure-environment-variables)
7. [Step 5: Verify Backend Deployment & Health Check](#step-5-verify-backend-deployment--health-check)
8. [Step 6: Deploy the Frontend & Connect to Backend](#step-6-deploy-the-frontend--connect-to-backend)
9. [Step 7: Full Application Test](#step-7-full-application-test)
10. [Troubleshooting Common Issues](#troubleshooting-common-issues)

---

## 1. Important Warnings & Free Tier Reality

> [!WARNING]
> **Render Free PostgreSQL Expiry**: Render's free PostgreSQL database automatically **expires after 30 days** and will be deleted.
> **Better Free Alternative**: Use [Neon.tech](https://neon.tech) or [Supabase](https://supabase.com). Both offer **permanent, generous free PostgreSQL databases** that never expire after 30 days. You can use their standard connection string with this project without changing any code.

> [!NOTE]
> **Render Free Web Service "Spin Down"**: On the Free plan, if no one visits your application for 15 minutes, Render puts the container to sleep to save resources. When someone makes a new request, Render automatically boots it back up. The very first request after sleeping may take **30 to 50 seconds** (known as a "cold start"). Subsequent requests are instant.

> [!CAUTION]
> **Revoke Any Exposed Secrets**: If you previously shared or pushed an API key (such as a Groq or Razorpay key) to a public repository, go to your provider dashboard and **revoke that key and generate a new one**. Never share your live keys with anyone.

---

## 2. Prerequisites

Before starting, ensure you have:
1. A **GitHub account** ([github.com](https://github.com)).
2. A **Render account** ([render.com](https://render.com)).
3. (Optional but recommended) A free database account on **Neon.tech** or **Supabase** (or you can use Render's built-in PostgreSQL).
4. Free API keys for services you plan to use:
   - **Groq API Key**: Free at [console.groq.com/keys](https://console.groq.com/keys)
   - **Razorpay API Keys**: Free sandbox at [dashboard.razorpay.com](https://dashboard.razorpay.com)
   - **Gmail App Password**: For OTP verification emails (generated in your Google Account security settings)

---

## Step 1: Push Your Code to GitHub

Make sure all latest changes are committed and pushed to your GitHub repository:

```bash
git status
git add .
git commit -m "chore: prepare for production deployment on Render"
git push origin main
```

---

## Step 2: Create a PostgreSQL Database

### Option A: Using Neon.tech (Recommended — Permanent Free Tier)
1. Go to [Neon.tech](https://neon.tech) and sign up for free.
2. Click **Create Project** (e.g. name it `shopsphere-db`).
3. On the project dashboard, copy the **Connection string** (select `Postgres` or `psql`).
4. The string looks like:
   `postgresql://username:password@ep-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require`
5. Save this URL. You will paste it into Render as `DATABASE_URL`.

### Option B: Using Render PostgreSQL
1. Log in to [dashboard.render.com](https://dashboard.render.com).
2. Click **New +** (top right) ➔ **PostgreSQL**.
3. Fill in:
   - **Name**: `shopsphere-db`
   - **Database**: `ecommerce_multivendor`
   - **Region**: Choose the region closest to you (e.g., Singapore, Oregon, Frankfurt).
   - **Plan**: Free
4. Click **Create Database**.
5. Once created, copy the **Internal Database URL** (or External Database URL if hosting frontend externally).

---

## Step 3: Create the Backend Web Service on Render

Because Spring Boot requires Java and Render does not have a native Java runtime, we deploy using **Docker**:

1. In the Render Dashboard, click **New +** ➔ **Web Service**.
2. Under "Connect a repository", select your repository (`yashlodam/ecommerce-multivendor`).
3. Fill in the service settings:
   - **Name**: `shopsphere-backend` (or any name you like)
   - **Region**: Same region as your database
   - **Branch**: `main`
   - **Language**: Select **`Docker`**
   - **Dockerfile Path**: `./Dockerfile`
   - **Instance Type**: **`Free`** (512 MB RAM, 0.1 CPU)

---

## Step 4: Configure Environment Variables

Scroll down to the **Environment Variables** section on Render, click **Add Environment Variable**, and add each of the following:

| Variable Key | Value to Enter | Why It's Needed |
| :--- | :--- | :--- |
| `DATABASE_URL` | *Your PostgreSQL URL from Step 2* | Database connection string. `DatabaseConfig.java` converts it to JDBC automatically. |
| `JWT_SECRET_KEY` | *A random string of 64+ characters* | Signs login tokens. Run `openssl rand -base64 64` or type a long random string. |
| `FRONTEND_URL` | `https://your-frontend.onrender.com` | Your live frontend domain so CORS allows API calls. (Can be updated after deploying frontend). |
| `COOKIE_SECURE` | `true` | Tells the browser cookies must use HTTPS in production. |
| `COOKIE_SAME_SITE` | `None` *(if frontend & backend have different domains)* | Allows auth cookies across domains. Set to `Lax` if served from the same domain. |
| `RAZORPAY_KEY_ID` | `rzp_test_...` *(or leave blank initially)* | From your Razorpay dashboard for checkout. |
| `RAZORPAY_KEY_SECRET` | *Your Razorpay secret key* | From your Razorpay dashboard. |
| `MAIL_USERNAME` | `your-email@gmail.com` | Email account to dispatch OTPs. |
| `MAIL_PASSWORD` | `your-16-char-app-password` | Google 16-character App Password (not your normal password). |
| `ADMIN_EMAIL` | `admin@shopsphere.com` | Seeds an initial administrator account on first start. |
| `ADMIN_PASSWORD` | *Your chosen strong admin password* | Admin password for first-time login. |
| `ADMIN_FULL_NAME` | `ShopSphere Admin` | Display name of the seed admin. |
| `GROQ_API_KEY` | `gsk_...` *(your new key from console.groq.com)* | Powers the AI Shopping Assistant. |
| `AI_RATE_LIMIT` | `60` | Limits AI requests per user/IP per minute. |

---

## Step 5: Configure Health Check & Deploy

1. Scroll down to **Advanced Settings**.
2. In **Health Check Path**, enter:
   ```
   /actuator/health
   ```
3. Click **Create Web Service**.
4. Render will start building your Docker container. You can watch the real-time build logs:
   - Downloading Maven dependencies
   - Compiling 184 Java source files
   - Generating `app.jar`
   - Booting Spring Boot on port `0.0.0.0:$PORT`
5. Once deployment finishes, Render shows **Live** with a green badge!
6. Copy your public URL (e.g. `https://shopsphere-backend.onrender.com`).

### Verify Deployment:
- Open: `https://shopsphere-backend.onrender.com/actuator/health` ➔ Should return: `{"status":"UP","groups":["liveness","readiness"]}`
- Open: `https://shopsphere-backend.onrender.com/swagger-ui/index.html` ➔ Interactive OpenAPI / Swagger documentation for testing your APIs.
- Open: `https://shopsphere-backend.onrender.com/home/categories` ➔ Returns the marketplace category JSON.

---

## Step 6: Deploy the Frontend & Connect to Backend

1. Go to your frontend repository:
   In `src/config/Api.js`, the frontend is already configured to read:
   ```javascript
   const API_URL = import.meta.env.VITE_API_URL || "";
   ```
2. In Render, create a **Static Site**:
   - Click **New +** ➔ **Static Site**.
   - Select your frontend repository.
   - **Build Command**: `npm run build`
   - **Publish Directory**: `dist`
3. Add the Environment Variable in your frontend service:
   - **Key**: `VITE_API_URL`
   - **Value**: `https://shopsphere-backend.onrender.com` *(your backend Render URL, no trailing slash)*
4. Click **Create Static Site**.
5. Once the frontend is deployed (e.g. `https://shopsphere-frontend.onrender.com`), go back to your **Backend Service** ➔ **Environment** tab:
   - Update `FRONTEND_URL` to match your frontend domain:
     `FRONTEND_URL=https://shopsphere-frontend.onrender.com`
   - Save changes (Render will trigger a quick reload).

---

## Step 7: Full Application Test

1. **Marketplace Browsing**: Open your frontend URL and verify categories, banners, and deals load.
2. **User Authentication**:
   - Register a new account or log in with OTP.
   - Or log in with the admin credentials set in `ADMIN_EMAIL` / `ADMIN_PASSWORD`.
3. **Cart & Wishlist**: Add products to cart and update quantities.
4. **AI Shopping Assistant**: Click the chat widget and ask: *"Show me top deals on electronics"*.
5. **Notifications**: Check the notification bell for real-time order and system alerts.

---

## Troubleshooting Common Issues

### 1. Cold Start Delay (502 / Bad Gateway on First Request)
- **Cause**: On Render's Free tier, the service sleeps after 15 minutes of inactivity. The first request wakes the container, which takes ~30-45 seconds.
- **Fix**: Wait 45 seconds and refresh. To keep it awake during demo presentations, you can use a free pinging service like UptimeRobot to ping `/actuator/health` every 10 minutes.

### 2. CORS Error in Browser Console
- **Cause**: The frontend URL does not match `FRONTEND_URL` or `APP_CORS_ALLOWED_ORIGINS` on the backend.
- **Fix**: In the backend Render dashboard, check that `FRONTEND_URL` has no trailing slash (e.g. `https://shopsphere.onrender.com`, NOT `https://shopsphere.onrender.com/`).

### 3. Cookies Not Stored Across Domains
- **Cause**: Cross-site cookie security in modern browsers (Chrome/Safari).
- **Fix**: Ensure both backend and frontend use `https://`, and verify `COOKIE_SECURE=true` and `COOKIE_SAME_SITE=None` in your backend environment variables.
