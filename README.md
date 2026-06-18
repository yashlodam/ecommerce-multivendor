# 🛒 ShopSphere - Multivendor E-Commerce Platform

A full-stack Multivendor E-Commerce Platform built using Spring Boot and React. This project allows customers to browse products, place orders, manage carts and wishlists, while sellers can manage products, orders, and earnings through a dedicated dashboard. Admins can manage sellers, coupons, deals, and home page content.

---

## 🚀 Features

### 👤 Customer Features

#### 🤖 AI Chatbot

Customers can interact with a chatbot to get information about:

* Order History
* Cart Items
* Product Details
* Store Policies

#### 🛍️ Product Management

* Browse Products
* Search Products
* Filter Products
* Sort Products
* Product Pagination
* Product Details Page

#### 🛒 Cart Management

* Add Product to Cart
* Update Quantity
* Remove Product from Cart

#### ❤️ Wishlist

* Add Product to Wishlist
* Remove Product from Wishlist
* View Wishlist Products

#### ⭐ Reviews & Ratings

* Write Product Reviews
* Give Ratings
* View Product Reviews

#### 💳 Checkout & Payments

* Apply Coupons
* Manage Shipping Addresses
* Razorpay Integration
* Stripe Integration

#### 📦 Order Management

* Place Orders
* View Order History
* Track Orders
* Cancel Orders

#### 👤 User Profile

* Manage Personal Information
* Manage Addresses
* View Purchase History

---

## 🏪 Seller Features

### 📊 Seller Dashboard

* Revenue Overview
* Earnings Analytics
* Order Statistics
* Sales Reports

### 📈 Reports

* Today's Earnings
* Last 7 Days Earnings
* Last 12 Months Earnings
* Total Sales
* Refund Statistics
* Cancellation Statistics

### 📦 Product Management

* Create Products
* Update Products
* Delete Products
* Manage Inventory

### 📑 Order Management

* View Seller Orders
* Update Order Status
* Manage Deliveries

### 💰 Payment & Transactions

* Track Payments
* Transaction History
* Revenue Reports

### 👤 Seller Profile

* Update Seller Details
* Business Information Management

---

## 👨‍💼 Admin Features

### 📊 Admin Dashboard

* Total Users
* Total Sellers
* Total Orders
* Total Revenue
* Total Products

### 🏪 Seller Management

* Approve Sellers
* Suspend Sellers
* Ban Sellers
* Manage Seller Accounts

### 🎟️ Coupon Management

* Create Coupons
* Update Coupons
* Delete Coupons
* Activate/Deactivate Coupons

### 🎯 Deal Management

* Create Deals
* Update Deals
* Delete Deals

### 🏠 Home Page Management

* Manage Home Categories
* Manage Banners
* Manage Featured Sections
* Customize Homepage Layout

### 👥 User Management

* View Users
* Ban Users
* Unban Users
* Manage User Accounts

---

## 🛠️ Tech Stack

### Backend

* Java
* Spring Boot
* Spring Security
* Spring Data JPA
* Hibernate
* JWT Authentication
* Java Mail Sender
* MySQL / PostgreSQL

### Frontend

* React JS
* Javascript
* Redux Toolkit
* Material UI (MUI)
* Tailwind CSS
* Axios
* React Router DOM
* Formik
* Yup
* React Charts

### Payment Integration

* Razorpay
* Stripe

---

## 🔐 Authentication & Security

* JWT Authentication
* Role-Based Authorization
* Spring Security
* OTP Login
* Email Verification

### Roles

* CUSTOMER
* SELLER
* ADMIN

---

## 📂 Project Structure

```text
Backend
├── Authentication
├── User Management
├── Seller Management
├── Product Management
├── Cart Management
├── Wishlist Management
├── Order Management
├── Coupon Management
├── Deal Management
├── Home Page Management
└── Payment Integration

Frontend
├── Customer Panel
├── Seller Dashboard
├── Admin Dashboard
└── Shared Components
```

---

## ⚡ Getting Started

### Clone Repository

```bash
git clone https://github.com/yourusername/ecommerce-multivendor.git
```

### Backend Setup

```bash
cd backend
```

Configure database credentials inside:

```properties
application.properties
```

Run:

```bash
mvn spring-boot:run
```

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

---

## 🎯 Future Enhancements

* AI Product Recommendation System
* AI Customer Support Chatbot
* Real-Time Notifications
* Seller Analytics Dashboard
* Product Recommendation Engine
* Inventory Forecasting
* Multi-language Support

---

## 👨‍💻 Author

**Yash Sunil Lodam**

Full Stack Developer

### Skills

* Java
* Spring Boot
* React JS
* MySQL
* PostgreSQL
* REST APIs
* JWT Authentication
* Git & GitHub

---

⭐ If you found this project useful, don't forget to star the repository.
