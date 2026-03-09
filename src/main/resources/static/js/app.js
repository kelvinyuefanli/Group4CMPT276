"use strict";

/* =================================================================
   Constants
   ================================================================= */
var DAYS = [
  "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY",
  "FRIDAY", "SATURDAY", "SUNDAY"
];

var DAY_LABELS = {
  MONDAY: "Monday", TUESDAY: "Tuesday", WEDNESDAY: "Wednesday",
  THURSDAY: "Thursday", FRIDAY: "Friday", SATURDAY: "Saturday", SUNDAY: "Sunday"
};

var DIET_OPTIONS = [
  "Vegetarian", "Vegan", "Gluten-Free", "Dairy-Free",
  "Nut-Free", "Low-Carb", "Halal", "Kosher"
];

var CUISINE_OPTIONS = [
  "Mediterranean", "Asian", "Mexican", "Indian",
  "Italian", "American", "Middle Eastern"
];

/* =================================================================
   Application State
   ================================================================= */
var state = {
  view: "plan",
  selectedMeal: null,
  mealPlan: null,
  checkedItems: {},
  servingSize: 2,
  selectedDiets: {},
  selectedCuisines: {},
  generating: false
};

/* =================================================================
   Helpers
   ================================================================= */
function esc(str) {
  if (str == null) return "";
  var div = document.createElement("div");
  div.appendChild(document.createTextNode(String(str)));
  return div.innerHTML;
}

function $(sel) { return document.querySelector(sel); }
function $$(sel) { return document.querySelectorAll(sel); }

/* =================================================================
   View Switching
   ================================================================= */
function switchView(view) {
  state.view = view;

  var views = { plan: "plan-view", grocery: "grocery-view", preferences: "preferences-view" };
  Object.keys(views).forEach(function (key) {
    var el = document.getElementById(views[key]);
    if (key === view) {
      el.removeAttribute("hidden");
    } else {
      el.setAttribute("hidden", "");
    }
  });

  var navBtns = $$("#main-nav .nav-btn");
  navBtns.forEach(function (btn) {
    if (btn.getAttribute("data-view") === view) {
      btn.classList.add("active");
    } else {
      btn.classList.remove("active");
    }
  });

  if (view === "grocery") loadGroceryList();
  if (view === "preferences") loadPreferences();
}

/* =================================================================
   Meal Plan Grid
   ================================================================= */
function buildSlots(meals) {
  var byDay = {};
  DAYS.forEach(function (d) {
    byDay[d] = {
      day: d,
      breakfast: null, breakfastId: null,
      lunch: null, lunchId: null,
      dinner: null, dinnerId: null
    };
  });
  if (meals) {
    meals.forEach(function (m) {
      var slot = byDay[m.dayOfWeek];
      if (!slot) return;
      if (m.mealType === "BREAKFAST") { slot.breakfast = m.recipeName; slot.breakfastId = m.recipeId; }
      if (m.mealType === "LUNCH")     { slot.lunch = m.recipeName;     slot.lunchId = m.recipeId; }
      if (m.mealType === "DINNER")    { slot.dinner = m.recipeName;    slot.dinnerId = m.recipeId; }
    });
  }
  return DAYS.map(function (d) { return byDay[d]; });
}

function renderMealGrid(meals) {
  var container = $("#meal-grid");
  var slots = buildSlots(meals);

  var html = '<div class="grid-header">' +
    "<span>Day</span><span>Breakfast</span><span>Lunch</span><span>Dinner</span>" +
    "</div>";

  slots.forEach(function (slot) {
    html += '<div class="grid-row">';
    html += '<div class="day-label">' + esc(DAY_LABELS[slot.day] || slot.day) + "</div>";

    ["breakfast", "lunch", "dinner"].forEach(function (type) {
      var name = slot[type];
      var recipeId = slot[type + "Id"];
      var sel = state.selectedMeal &&
                state.selectedMeal.day === slot.day &&
                state.selectedMeal.type === type;
      html += '<button class="meal-cell' + (sel ? " selected" : "") + '"' +
        ' data-day="' + esc(slot.day) + '"' +
        ' data-type="' + esc(type) + '"' +
        ' data-name="' + esc(name || "\u2014") + '"' +
        ' data-recipe-id="' + (recipeId != null ? recipeId : "") + '">' +
        esc(name || "\u2014") +
        "</button>";
    });

    html += "</div>";
  });

  container.innerHTML = html;

  container.querySelectorAll(".meal-cell").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var rid = btn.getAttribute("data-recipe-id");
      selectMeal(
        btn.getAttribute("data-day"),
        btn.getAttribute("data-type"),
        btn.getAttribute("data-name"),
        rid ? Number(rid) : null
      );
    });
  });
}

function selectMeal(day, type, name, recipeId) {
  state.selectedMeal = { day: day, type: type, name: name, recipeId: recipeId };

  $$(".meal-cell").forEach(function (c) {
    if (c.getAttribute("data-day") === day && c.getAttribute("data-type") === type) {
      c.classList.add("selected");
    } else {
      c.classList.remove("selected");
    }
  });

  renderRecipePanel();
}

/* =================================================================
   Recipe Detail
   ================================================================= */
function renderRecipePanel() {
  var panel = $("#recipe-panel");
  var meal = state.selectedMeal;

  if (!meal) {
    panel.innerHTML =
      '<div class="empty-state"><div>' +
      "<p>Select a meal from the plan</p>" +
      "<p>Recipe details will appear here</p>" +
      "</div></div>";
    return;
  }

  var dayLabel = DAY_LABELS[meal.day] || meal.day;
  panel.innerHTML =
    '<div class="recipe-detail">' +
    '<p class="section-label mb-1">' + esc(dayLabel) + " &middot; " + esc(meal.type) + "</p>" +
    '<h2 style="font-size:1.5rem;font-weight:600;" class="mb-6">' + esc(meal.name) + "</h2>" +
    '<p class="text-muted">Loading recipe&hellip;</p>' +
    "</div>";

  if (meal.recipeId == null) {
    panel.querySelector(".text-muted").innerHTML =
      '<span class="font-serif" style="font-style:italic;">Recipe details for &ldquo;' +
      esc(meal.name) + '&rdquo; will appear here once generated.</span>';
    return;
  }

  Api.fetchRecipe(meal.recipeId).then(function (recipe) {
    if (state.selectedMeal && state.selectedMeal.recipeId !== recipe.id) return;
    renderRecipeDetail(recipe);
  }).catch(function () {
    panel.querySelector(".text-muted").textContent = "Failed to load recipe.";
  });
}

function renderRecipeDetail(recipe) {
  var panel = $("#recipe-panel");
  var meal = state.selectedMeal;
  var dayLabel = DAY_LABELS[meal.day] || meal.day;

  var html = '<div class="recipe-detail">';
  html += '<p class="section-label mb-1">' + esc(dayLabel) + " &middot; " + esc(meal.type) + "</p>";
  html += '<h2 style="font-size:1.5rem;font-weight:600;" class="mb-6">' + esc(recipe.title) + "</h2>";

  var metaParts = [];
  if (recipe.servings) metaParts.push("Serves " + recipe.servings);
  if (recipe.cookTimeMinutes) metaParts.push("Cook: " + recipe.cookTimeMinutes + " min");
  if (recipe.cuisine) metaParts.push(esc(recipe.cuisine));
  if (metaParts.length) {
    html += '<div class="recipe-meta">' + metaParts.map(function (s) { return "<span>" + s + "</span>"; }).join("") + "</div>";
  }

  if (recipe.ingredients && recipe.ingredients.length) {
    html += '<div class="mb-8">';
    html += '<h3 class="section-label mb-3">Ingredients</h3>';
    html += '<ul class="ingredient-list">';
    recipe.ingredients.forEach(function (ing) {
      var text = "";
      if (ing.quantity != null) text += ing.quantity + " ";
      if (ing.unit != null) text += ing.unit + " ";
      text += ing.name;
      html += '<li class="ingredient-item">' +
        '<span class="ingredient-bullet"></span>' +
        esc(text) +
        "</li>";
    });
    html += "</ul></div>";
  }

  if (recipe.instructions) {
    var steps = recipe.instructions.split(/\d+\.\s*/).filter(Boolean);
    html += "<div>";
    html += '<h3 class="section-label mb-3">Instructions</h3>';
    html += '<ol class="instruction-list">';
    steps.forEach(function (step, i) {
      html += '<li class="instruction-step">' +
        '<span class="step-number">' + (i + 1) + "</span>" +
        "<span>" + esc(step.trim()) + "</span>" +
        "</li>";
    });
    html += "</ol></div>";
  }

  html += "</div>";
  panel.innerHTML = html;
}

/* =================================================================
   Grocery List
   ================================================================= */
function loadGroceryList() {
  var container = $("#grocery-content");
  container.innerHTML =
    '<h2 style="font-size:1.5rem;font-weight:600;" class="mb-2">Grocery List</h2>' +
    '<p class="text-sm text-muted">Loading grocery list&hellip;</p>';

  Api.fetchGroceryList().then(function (data) {
    renderGroceryList(data.items || []);
  }).catch(function () {
    container.querySelector(".text-muted").textContent = "Failed to load grocery list.";
  });
}

function renderGroceryList(items) {
  var container = $("#grocery-content");

  if (!items.length) {
    container.innerHTML =
      '<h2 style="font-size:1.5rem;font-weight:600;" class="mb-2">Grocery List</h2>' +
      '<p class="text-sm text-muted">Generate a meal plan first to see your grocery list.</p>';
    return;
  }

  var checkedCount = items.filter(function (it) { return !!state.checkedItems[it.name]; }).length;
  var categories = [];
  var seen = {};
  items.forEach(function (it) {
    if (!seen[it.category]) { seen[it.category] = true; categories.push(it.category); }
  });

  var html = '<div style="animation: fadeIn 0.3s ease-out;">';
  html += '<div class="mb-6">';
  html += '<h2 style="font-size:1.5rem;font-weight:600;">Grocery List</h2>';
  html += '<p class="text-sm text-muted mt-1">' +
    checkedCount + " of " + items.length + " items checked &middot; Aggregated from 7-day plan</p>";
  html += "</div>";

  categories.forEach(function (cat) {
    html += '<div class="grocery-category">';
    html += '<h3 class="category-header">' + esc(cat) + "</h3>";
    html += "<ul>";
    items.filter(function (it) { return it.category === cat; }).forEach(function (item) {
      var isChecked = !!state.checkedItems[item.name];
      html += "<li>" +
        '<button class="grocery-item' + (isChecked ? " checked" : "") +
        '" data-name="' + esc(item.name) + '">' +
        '<span class="grocery-check">' +
        '<svg width="10" height="8" viewBox="0 0 10 8" fill="none">' +
        '<path d="M1 4L3.5 6.5L9 1" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>' +
        "</svg></span>" +
        '<span class="grocery-name">' + esc(item.name) + "</span>" +
        '<span class="grocery-qty">' + esc(item.quantity) + "</span>" +
        "</button></li>";
    });
    html += "</ul></div>";
  });

  html += '<div class="grocery-footer">';
  html += '<button class="btn-primary btn-primary-full">Send to Instacart &rarr;</button>';
  html += '<p style="font-size:0.75rem;color:var(--muted-foreground);text-align:center;margin-top:0.5rem;">' +
    "Opens Instacart with your items pre-filled</p>";
  html += "</div></div>";

  container.innerHTML = html;

  container.querySelectorAll(".grocery-item").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var name = btn.getAttribute("data-name");
      state.checkedItems[name] = !state.checkedItems[name];
      renderGroceryList(items);
    });
  });
}

/* =================================================================
   Preferences
   ================================================================= */
function loadPreferences() {
  Api.fetchPreferences().then(function (prefs) {
    if (prefs.servingSize) state.servingSize = prefs.servingSize;
    if (prefs.dietaryRestrictions) {
      state.selectedDiets = {};
      prefs.dietaryRestrictions.split(",").forEach(function (d) {
        var trimmed = d.trim();
        if (trimmed) state.selectedDiets[trimmed] = true;
      });
    }
    if (prefs.preferredCuisines) {
      state.selectedCuisines = {};
      prefs.preferredCuisines.split(",").forEach(function (c) {
        var trimmed = c.trim();
        if (trimmed) state.selectedCuisines[trimmed] = true;
      });
    }
    renderPreferences();
  }).catch(function () {
    renderPreferences();
  });
}

function renderPreferences() {
  renderServingButtons();
  renderDietChips();
  renderCuisineChips();
}

function renderServingButtons() {
  var container = $("#serving-btns");
  var html = "";
  for (var n = 1; n <= 6; n++) {
    html += '<button class="serving-btn' + (n === state.servingSize ? " active" : "") +
      '" data-n="' + n + '">' + n + "</button>";
  }
  container.innerHTML = html;
  container.querySelectorAll(".serving-btn").forEach(function (btn) {
    btn.addEventListener("click", function () {
      state.servingSize = Number(btn.getAttribute("data-n"));
      renderServingButtons();
      Api.updatePreferences({ servingSize: state.servingSize });
    });
  });
}

function renderDietChips() {
  var container = $("#diet-chips");
  var html = "";
  DIET_OPTIONS.forEach(function (diet) {
    var active = !!state.selectedDiets[diet];
    html += '<button class="chip' + (active ? " active" : "") +
      '" data-diet="' + esc(diet) + '">' + esc(diet) + "</button>";
  });
  container.innerHTML = html;
  container.querySelectorAll(".chip").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var diet = btn.getAttribute("data-diet");
      state.selectedDiets[diet] = !state.selectedDiets[diet];
      if (!state.selectedDiets[diet]) delete state.selectedDiets[diet];
      renderDietChips();
      var dietStr = Object.keys(state.selectedDiets).join(", ") || null;
      Api.updatePreferences({ dietaryRestrictions: dietStr });
    });
  });
}

function renderCuisineChips() {
  var container = $("#cuisine-chips");
  var html = "";
  CUISINE_OPTIONS.forEach(function (cuisine) {
    var active = !!state.selectedCuisines[cuisine];
    html += '<button class="chip' + (active ? " active" : "") +
      '" data-cuisine="' + esc(cuisine) + '">' + esc(cuisine) + "</button>";
  });
  container.innerHTML = html;
  container.querySelectorAll(".chip").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var cuisine = btn.getAttribute("data-cuisine");
      state.selectedCuisines[cuisine] = !state.selectedCuisines[cuisine];
      if (!state.selectedCuisines[cuisine]) delete state.selectedCuisines[cuisine];
      renderCuisineChips();
      var cuisineStr = Object.keys(state.selectedCuisines).join(", ") || null;
      Api.updatePreferences({ preferredCuisines: cuisineStr });
    });
  });
}

/* =================================================================
   Generate Meal Plan
   ================================================================= */
function handleGenerate() {
  if (state.generating) return;
  state.generating = true;
  var btn = $("#generate-btn");
  btn.textContent = "Generating\u2026";
  btn.disabled = true;

  Api.generateMealPlan("").then(function (plan) {
    state.mealPlan = plan;
    state.selectedMeal = null;
    state.checkedItems = {};
    updatePlanSubtitle();
    renderMealGrid(plan.meals || []);
    $("#recipe-panel").innerHTML =
      '<div class="empty-state"><div>' +
      "<p>Select a meal from the plan</p>" +
      "<p>Recipe details will appear here</p>" +
      "</div></div>";
  }).catch(function (err) {
    console.error("Generation failed:", err);
    var panel = $("#recipe-panel");
    panel.innerHTML =
      '<div class="empty-state"><div>' +
      '<p style="color:var(--destructive,#c44);">Failed to generate meal plan</p>' +
      "<p>Check your internet connection and GEMINI_API_KEY, then try again.</p>" +
      "</div></div>";
  }).finally(function () {
    state.generating = false;
    btn.textContent = "Generate New Plan";
    btn.disabled = false;
  });
}

/* =================================================================
   Plan Subtitle
   ================================================================= */
function updatePlanSubtitle() {
  var el = $("#plan-subtitle");
  var plan = state.mealPlan;
  if (!plan || !plan.meals || !plan.meals.length) {
    el.innerHTML = "No meal plan yet &mdash; generate one from Preferences";
    return;
  }
  var text = plan.weekStartDate
    ? "Week of " + plan.weekStartDate
    : "Current plan";
  text += " \u00B7 Select a meal to view details";
  el.textContent = text;
}

/* =================================================================
   Initialization
   ================================================================= */
document.addEventListener("DOMContentLoaded", function () {
  $$("#main-nav .nav-btn").forEach(function (btn) {
    btn.addEventListener("click", function () {
      switchView(btn.getAttribute("data-view"));
    });
  });

  $("#generate-btn").addEventListener("click", handleGenerate);

  Onboarding.check().then(function (shown) {
    if (!shown) {
      initApp();
    }
  }).catch(function () {
    initApp();
  });
});

function initApp() {
  Api.fetchMealPlan().then(function (plan) {
    state.mealPlan = plan;
    updatePlanSubtitle();
    renderMealGrid(plan.meals || []);
  }).catch(function () {
    renderMealGrid([]);
  });

  renderPreferences();
}
