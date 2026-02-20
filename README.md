# SmartCart: An Intelligent Meal Planning and Grocery Integration Platform

SmartCart: An Intelligent Meal Planning and Grocery Integration Platform
Application Name
The name of our web application is SmartCart. SmartCart is a browser-based meal planning and grocery integration platform designed to simplify weekly meal organization and grocery preparation through intelligent automation and external API integration.

Problem Definition
Many individuals struggle with the time and cognitive effort required to plan meals for an entire week while also ensuring dietary restrictions, budget limits, and ingredient efficiency are respected. Users often browse multiple recipe websites, manually compare ingredient lists, and create grocery lists by hand. This process frequently results in duplicated ingredients, forgotten items, wasted food, and overspending. For individuals with allergies or dietary constraints, the task becomes even more complex and error-prone. The core problem SmartCart addresses is the fragmentation and inefficiency of the meal planning and grocery preparation process.

Current Solutions and Competitive Analysis
Currently, users rely on separate tools to accomplish meal planning. Recipe platforms such as AllRecipes provide access to individual recipes but do not generate cohesive weekly plans or consolidated grocery lists. Grocery store applications allow users to manually build carts, yet they do not intelligently derive grocery lists from selected meals. Meal kit subscription services attempt to solve the convenience problem by delivering pre-selected meals and ingredients, but they are expensive, restrictive, and limit user flexibility.
None of these systems fully integrate meal planning, grocery aggregation, pantry awareness, and real-world product matching into a single unified workflow. SmartCart improves upon existing solutions by connecting intelligent meal planning directly to structured grocery list generation and Instacart-based cart handoff, creating a streamlined and customizable experience.

Value and Impact
SmartCart improves users' lives by reducing time spent planning meals and preparing grocery lists. By automatically generating a weekly plan that respects dietary preferences and constraints, and by aggregating ingredients into a normalized and deduplicated grocery list, the application minimizes food waste and reduces overspending. The integration with Instacart enables users to seamlessly transition from planning to purchasing without manually re-entering grocery items.
The project is both practical and educational. It addresses a real-world organizational problem while demonstrating advanced full-stack engineering concepts, including authentication systems, REST API integration, asynchronous workflows, database modeling, and structured validation of large language model outputs.

Target Audience
The target audience for SmartCart includes university students, working professionals, families, and individuals with dietary restrictions who wish to streamline meal planning and grocery preparation. These users value efficiency, customization, and cost awareness. The application is designed for practical use rather than entertainment, focusing on productivity and organization.

Technical Stack
SmartCart will be implemented using the following technologies:
The backend will be developed in Java using Spring Boot, providing RESTful endpoints, authentication, and workflow orchestration. The application will use Postgres SQL as its relational database, hosted on Render. Data persistence will include user accounts, preferences, meal plans, recipes, grocery lists, and API responses.
The application will be deployed on Render.com as a web service. Environment variables will be used to securely store API keys and database credentials. Database schema migrations will be managed through a migration tool such as Flyway to ensure consistent deployment.
Version control will be handled using Git, with a public GitHub repository. All group members will have push access, and contributions will be tracked through commit history.
For external integrations, SmartCart will use two REST APIs. The first is the Google Gemini API, specifically the generateContent endpoint, which will be used to generate structured recipes and meal plans. The second is the Instacart Developer Platform API, including the GET /idp/v1/retailers endpoint to retrieve nearby retailers and the POST /idp/v1/products/recipe or POST /idp/v1/products/products_link endpoints to generate grocery handoff links. All API communication will occur via HTTP/HTTPS calls from the Spring Boot backend, and no sensitive financial data will be handled by our system.

Scope of the Project
The scope of SmartCart includes building a secure browser-based application with login functionality, structured meal planning workflows, grocery list aggregation, and external REST API integration. Users will be able to register and log in, configure dietary preferences, generate weekly meal plans, view recipes, access consolidated grocery lists, manage pantry items, and generate Instacart shopping links.
The project includes backend logic, database design, frontend interfaces, API integration, deployment configuration, and validation mechanisms. Checkout and payment processing will not be handled by SmartCart; instead, Instacart links will redirect users externally.

Core Feature Structure and Epics
SmartCart consists of multiple major subsystems, or epics, rather than a single isolated feature. Each epic represents a substantial development area appropriate for a five-person team.
The first epic is Authentication and User Management. Users can register, log in, and store personal dietary preferences, serving sizes, and restrictions. This includes implementing secure password storage, session management, and database persistence.
The second epic is Intelligent Meal Planning and Recipe Generation. Users can generate a weekly meal plan based on constraints. The system will use the Google Gemini API to generate structured recipe data, which will be validated server-side to ensure schema correctness and dietary compliance. Users can also swap individual meals, triggering partial regeneration.
The third epic is Ingredient Normalization and Grocery Aggregation. Users can view a consolidated grocery list where duplicate ingredients across recipes are merged, quantities are normalized, and pantry items are subtracted. This subsystem includes unit conversion logic and canonical ingredient mapping.
The fourth epic is Instacart API Integration and Cart Handoff. Users can match their grocery list to real product data and generate an Instacart-hosted shopping link. The system will retrieve retailer information via REST calls and generate product links without handling sensitive payment information.
The fifth epic is Database Architecture, Deployment, and Infrastructure Management. This includes Postgres schema design, migration management, environment configuration, logging, and deployment to Render.

Workload Distribution and Feasibility
The workload required for SmartCart is sufficient for four members including one extra epic split amongst all of us. Each epic represents a major subsystem requiring backend development, database modeling, API integration, testing, and documentation. Authentication requires secure implementation. The meal planning subsystem involves structured workflow orchestration and LLM validation. Grocery aggregation requires algorithmic processing and normalization logic. API integration requires HTTP communication and response parsing. Deployment and infrastructure require configuration and maintenance.
The complexity and technical depth of these components ensure balanced and meaningful work distribution across the team.

Conclusion
SmartCart is a technically rigorous and practically valuable web application that addresses a clear and common organizational problem. By integrating intelligent meal planning, grocery aggregation, and external REST APIs into a cohesive system, the project improves upon fragmented existing solutions. The application satisfies all course requirements, including secure login functionality and external web API integration via HTTP calls, while demonstrating advanced full-stack software engineering practices.

## Planned stack
- Backend: Java + Spring Boot (REST)
- Database: Postgres (Render)
- Migrations: Flyway (planned)
- External APIs: Google Gemini (generateContent), Instacart Developer Platform
- Deployment: Render.com

## Docs
See `docs/` for placeholders: overview, architecture, API contract, DB, deployment, and security notes.
