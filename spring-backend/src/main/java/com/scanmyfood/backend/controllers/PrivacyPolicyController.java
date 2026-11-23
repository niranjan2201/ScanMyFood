package com.scanmyfood.backend.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrivacyPolicyController {

    @GetMapping(value = {"/privacy-policy", "/privacy"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getPrivacyPolicy() {
        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Privacy Policy - ScanMyFood</title>
                <style>
                    :root {
                        --primary: #10b981;
                        --primary-dark: #059669;
                        --text-main: #1f2937;
                        --text-muted: #4b5563;
                        --bg-light: #f9fafb;
                        --card-bg: #ffffff;
                        --border: #e5e7eb;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        line-height: 1.6;
                        color: var(--text-main);
                        background-color: var(--bg-light);
                        margin: 0;
                        padding: 20px;
                    }
                    .container {
                        max-width: 860px;
                        margin: 40px auto;
                        background: var(--card-bg);
                        padding: 40px;
                        border-radius: 12px;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
                        border: 1px solid var(--border);
                    }
                    header {
                        border-bottom: 2px solid var(--border);
                        padding-bottom: 24px;
                        margin-bottom: 32px;
                    }
                    h1 {
                        color: var(--primary-dark);
                        font-size: 2rem;
                        margin: 0 0 8px 0;
                    }
                    .app-id {
                        font-family: monospace;
                        background: #ecfdf5;
                        color: var(--primary-dark);
                        padding: 4px 8px;
                        border-radius: 4px;
                        font-size: 0.9rem;
                    }
                    .effective-date {
                        color: var(--text-muted);
                        font-size: 0.95rem;
                        margin-top: 8px;
                    }
                    h2 {
                        color: #111827;
                        font-size: 1.35rem;
                        margin-top: 32px;
                        margin-bottom: 12px;
                        border-bottom: 1px solid #f3f4f6;
                        padding-bottom: 6px;
                    }
                    p, li {
                        color: var(--text-muted);
                        font-size: 1rem;
                    }
                    ul {
                        padding-left: 24px;
                    }
                    li {
                        margin-bottom: 8px;
                    }
                    .highlight-box {
                        background: #f0fdf4;
                        border-left: 4px solid var(--primary);
                        padding: 16px;
                        margin: 20px 0;
                        border-radius: 0 8px 8px 0;
                    }
                    footer {
                        margin-top: 48px;
                        padding-top: 24px;
                        border-top: 1px solid var(--border);
                        font-size: 0.9rem;
                        color: var(--text-muted);
                        text-align: center;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <header>
                        <h1>Privacy Policy for ScanMyFood</h1>
                        <div>Application ID: <span class="app-id">com.scanmyfood.app</span></div>
                        <div class="effective-date">Effective Date: November 24, 2026</div>
                    </header>

                    <div class="highlight-box">
                        <strong>Overview:</strong> ScanMyFood ("we", "our", or "the App") respects your privacy. This Privacy Policy discloses what data we collect, how it is used to analyze meals and product nutrition, and how your data is protected.
                    </div>

                    <h2>1. Information We Collect</h2>
                    <p>When you use ScanMyFood, we may collect the following types of information:</p>
                    <ul>
                        <li><strong>Account Information:</strong> Your email address and unique user identifier when you register or sign in via Firebase Authentication.</li>
                        <li><strong>Health & Nutritional Profile:</strong> Optional physical and health metrics you provide during onboarding or in your profile settings, including weight, height, dietary preferences (e.g., Vegetarian, Vegan, Non-Vegetarian), nutritional goals (e.g., Muscle Gain, Weight Loss), and self-declared health conditions (e.g., Diabetes, Hypertension, PCOS).</li>
                        <li><strong>Photos & Camera Data:</strong> Images of food meals and food packaging nutrition labels captured by you using your device's camera or uploaded from your photo gallery.</li>
                        <li><strong>Food Intake & Meal Logs:</strong> Records of analyzed meals, estimated calorie and nutrient values, and consumption timestamps you choose to save.</li>
                    </ul>

                    <h2>2. How We Use Your Information</h2>
                    <p>We use the collected information exclusively to provide and improve the services offered by ScanMyFood:</p>
                    <ul>
                        <li>To identify visible food items and estimate macronutrients (calories, protein, carbohydrates, fats, dietary fiber, sugars, and sodium).</li>
                        <li>To read and parse nutrition fact labels from product packaging images.</li>
                        <li>To provide personalized, AI-driven dietary feedback and nutritional recommendations tailored to your goals and health conditions.</li>
                        <li>To maintain your personal daily intake log and nutrition history across your sessions.</li>
                    </ul>

                    <h2>3. AI Processing & Third-Party Services</h2>
                    <p>To deliver advanced meal and nutrition analysis, ScanMyFood integrates trusted third-party service providers:</p>
                    <ul>
                        <li><strong>Google Gemini AI API:</strong> Images and meal descriptions submitted for analysis are processed via Google's Gemini multimodal AI service to extract nutritional information. Google does not use data submitted through the paid Gemini API to train foundational models.</li>
                        <li><strong>Google Firebase:</strong> Used for secure user authentication (Firebase Auth) and cloud backend management.</li>
                        <li><strong>Cloud Hosting Infrastructure (Railway):</strong> Used to securely host the backend application and databases with encrypted transit (HTTPS/SSL).</li>
                    </ul>
                    <p>We do not sell, rent, or trade your personal or health data to any advertising networks, data brokers, or third parties.</p>

                    <h2>4. Data Storage & Security</h2>
                    <p>We implement standard security measures, including HTTPS encryption in transit and access controls, to safeguard your information. Your health preferences and logged intakes are associated solely with your private user account.</p>

                    <h2>5. Data Retention & Deletion</h2>
                    <p>You have full control over your personal data:</p>
                    <ul>
                        <li><strong>Meal Log Deletion:</strong> You can delete any saved meal intake record directly within the App's history screen at any time.</li>
                        <li><strong>Account & Data Deletion:</strong> You may request the permanent deletion of your account and all associated data by sending an email to our support team at <a href="mailto:support@scanmyfood.app">support@scanmyfood.app</a> or <a href="mailto:naikniranjan300305@gmail.com">naikniranjan300305@gmail.com</a>. Upon verification, all your profile data and meal logs will be permanently deleted from our servers within 30 days.</li>
                    </ul>

                    <h2>6. Children's Privacy</h2>
                    <p>ScanMyFood is not directed to individuals under the age of 13. We do not knowingly collect personal information from children under 13.</p>

                    <h2>7. Changes to This Privacy Policy</h2>
                    <p>We may update our Privacy Policy periodically. We will notify users of any material changes by updating the "Effective Date" at the top of this policy and publishing the revised version on this page.</p>

                    <h2>8. Contact Us</h2>
                    <p>If you have any questions, concerns, or requests regarding this Privacy Policy or your data, please contact us at:</p>
                    <ul>
                        <li><strong>Email:</strong> <a href="mailto:naikniranjan300305@gmail.com">naikniranjan300305@gmail.com</a></li>
                        <li><strong>Developer:</strong> ScanMyFood App Team</li>
                    </ul>

                    <footer>
                        &copy; 2026 ScanMyFood. All rights reserved.
                    </footer>
                </div>
            </body>
            </html>
            """;
        return ResponseEntity.ok(html);
    }
}
