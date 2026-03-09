# SmartCart - Gemini API Integration

## Overview
SmartCart now includes AI-powered meal planning using Google's Gemini API. Users can enter dietary preferences and receive a personalized weekly meal plan.

## Setup Instructions

### 1. Get a Gemini API Key
1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Get API Key" or "Create API Key"
4. Copy your API key

### 2. Configure the API Key

You have two options:

**Option A: Environment Variable (Recommended)**
```bash
# Windows PowerShell
$env:GEMINI_API_KEY="your-api-key-here"

# Windows Command Prompt
set GEMINI_API_KEY=your-api-key-here

# Mac/Linux
export GEMINI_API_KEY="your-api-key-here"
```

**Option B: Update application.properties**
Edit `src/main/resources/application.properties`:
```properties
gemini.api.key=your-actual-api-key-here
```

### 3. Run the Application

```bash
# Navigate to the project directory
cd c:\Users\cohen\Documents\CMPT276V2\group_project\smartcart-web

# Run with Maven Wrapper
.\mvnw.cmd spring-boot:run
```

### 4. Access the Application
Open your browser and go to: `http://localhost:8080`

## Using the Meal Planner

1. Click "Meal Plan" in the sidebar navigation
2. Enter your dietary preferences (e.g., "Vegan", "Keto", "Gluten-free", "High protein")
3. Click "Generate AI Plan"
4. Wait a few seconds for Gemini to generate your personalized meal plan
5. The AI-generated table will appear with meals for the entire week

## Project Structure

```
smartcart-web/
├── src/main/java/com/smartcart/web/
│   ├── SmartcartWebApplication.java    # Main Spring Boot application
│   ├── controller/
│   │   └── PageController.java         # Handles web requests and meal generation
│   └── service/
│       └── GeminiService.java          # Integrates with Gemini API
├── src/main/resources/
│   ├── application.properties          # Configuration (API key)
│   ├── templates/
│   │   └── index.html                  # Main UI with meal planner form
│   └── static/
│       └── css/
│           └── styles.css              # Custom styling
└── pom.xml                             # Maven dependencies
```

## Features Implemented

✅ Server-side rendering (Java + Thymeleaf, no client-side JavaScript)  
✅ Gemini API integration for meal plan generation  
✅ Dietary preference customization  
✅ AI-generated HTML table with Tailwind CSS styling  
✅ Fallback to static meal plan when no AI plan is generated  
✅ Error handling for API failures  

## Technologies Used

- **Java 17**
- **Spring Boot 3.5.10**
- **Thymeleaf** (server-side templating)
- **Google Gemini API** (gemini-1.5-flash model)
- **Tailwind CSS** (styling for AI-generated content)
- **Maven** (dependency management)

## API Details

- **Endpoint**: `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent`
- **Model**: gemini-1.5-flash
- **Request**: JSON payload with user preferences
- **Response**: HTML table with weekly meal plan

## Troubleshooting

**Error: "Error generating plan. Please check API Key."**
- Verify your API key is correct
- Ensure the GEMINI_API_KEY environment variable is set
- Check your internet connection
- Verify the API key has proper permissions in Google Cloud Console

**Port 8080 already in use**
- Stop any other Spring Boot apps running
- Or change the port in `application.properties`: `server.port=8081`

**Maven not found**
- Use the Maven Wrapper: `.\mvnw.cmd` (Windows) or `./mvnw` (Mac/Linux)

## Next Steps

Consider adding:
- Database persistence for generated meal plans
- User authentication and saved preferences
- Grocery list generation from meal plans
- Recipe detail pages
- Nutritional information
- Cost estimation per meal
