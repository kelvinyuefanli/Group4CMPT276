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
  "Italian", "Mexican", "Chinese", "Japanese", "Korean",
  "Thai", "Vietnamese", "Indian", "Mediterranean", "French",
  "American", "Middle Eastern", "Greek", "Caribbean", "Spanish"
];

var PROTEIN_OPTIONS = [
  { name: "Poultry", items: ["Chicken Breast", "Chicken Thighs", "Ground Chicken", "Turkey Breast", "Ground Turkey"] },
  { name: "Beef & Pork", items: ["Ground Beef", "Beef Steak", "Beef Stew Meat", "Pork Chops", "Ground Pork", "Bacon", "Sausage"] },
  { name: "Seafood", items: ["Salmon", "Shrimp", "Tilapia", "Tuna", "Cod"] },
  { name: "Other Protein", items: ["Eggs", "Tofu", "Tempeh", "Black Beans", "Chickpeas", "Lentils"] }
];

var VEGETABLE_OPTIONS = [
  { name: "Leafy Greens", items: ["Spinach", "Kale", "Romaine Lettuce", "Mixed Greens"] },
  { name: "Cruciferous", items: ["Broccoli", "Cauliflower", "Brussels Sprouts", "Cabbage"] },
  { name: "Everyday Veggies", items: ["Bell Peppers", "Zucchini", "Carrots", "Tomatoes", "Cucumber", "Green Beans", "Corn", "Peas"] },
  { name: "Root & Starchy", items: ["Sweet Potatoes", "Potatoes", "Butternut Squash", "Beets"] },
  { name: "Alliums & Aromatics", items: ["Mushrooms", "Asparagus", "Celery", "Eggplant"] }
];

var FRUIT_OPTIONS = [
  "Bananas", "Apples", "Berries", "Oranges", "Grapes",
  "Strawberries", "Blueberries", "Avocado", "Mango", "Pineapple",
  "Peaches", "Pears", "Lemons", "Limes", "Watermelon"
];

var CHECK_SVG = '<svg width="10" height="8" viewBox="0 0 10 8" fill="none"><path d="M1 4L3.5 6.5L9 1" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';

/* =================================================================
   Application State
   ================================================================= */
var state = {
  view: "plan",
  selectedMeal: null,
  swapSelections: {},  // key: "DAY_TYPE" -> true
  swapping: false,
  mealPlan: null,
  checkedItems: {},
  groceryItemIndex: {},
  servingSize: 2,
  selectedDiets: {},
  selectedCuisines: {},
  selectedProteins: {},
  selectedVegetables: {},
  selectedFruits: {},
  pantrySaving: {},
  generating: false
};

/* =================================================================
   Auth header (login / logout in main UI)
   ================================================================= */
function updateAuthHeader(authData) {
  var el = $("#auth-header");
  if (!el) return;

  function render(data) {
    if (data.loggedIn && data.email) {
      var html = "";
      if (data.isAdmin) {
        html += '<a href="/admin.html" class="auth-link" style="font-weight:600;">Admin</a> ';
      }
      html += '<span class="auth-email">' + esc(data.email) + "</span> " +
        '<a href="/logout" class="auth-link">Logout</a>';
      el.innerHTML = html;
    } else {
      el.innerHTML = '<a href="/login" class="auth-link">Login</a> ' +
        '<a href="/register" class="auth-link">Register</a>';
    }
  }

  if (authData) {
    render(authData);
    return;
  }

  fetch("/api/auth/me", { credentials: "same-origin" })
    .then(function (res) { return res.ok ? res.json() : { loggedIn: false }; })
    .then(render)
    .catch(function () {
      el.innerHTML = '<a href="/login" class="auth-link">Login</a> ' +
        '<a href="/register" class="auth-link">Register</a>';
    });
}

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

  // Swap toolbar
  var swapCount = Object.keys(state.swapSelections).length;
  var toolbarHtml = '<div class="swap-toolbar" id="swap-toolbar"' +
    (swapCount > 0 ? '' : ' style="display:none"') + '>' +
    '<span>' + swapCount + ' meal' + (swapCount !== 1 ? 's' : '') + ' selected</span>' +
    '<button class="btn-swap" id="btn-swap-selected"' +
    (state.swapping ? ' disabled' : '') + '>' +
    (state.swapping ? 'Swapping...' : 'Swap Selected') + '</button>' +
    '<button class="btn-swap-cancel" id="btn-swap-cancel">Clear</button>' +
    '</div>';

  var html = toolbarHtml;
  html += '<div class="grid-header">' +
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
      var swapKey = slot.day + "_" + type.toUpperCase();
      var swapChecked = !!state.swapSelections[swapKey];
      html += '<button class="meal-cell' + (sel ? " selected" : "") +
        (swapChecked ? " swap-checked" : "") +
        (state.swapping ? " swapping" : "") + '"' +
        ' data-day="' + esc(slot.day) + '"' +
        ' data-type="' + esc(type) + '"' +
        ' data-name="' + esc(name || "\u2014") + '"' +
        ' data-recipe-id="' + (recipeId != null ? recipeId : "") + '">' +
        (swapChecked ? '<span class="swap-check-icon">&#10003;</span>' : '') +
        esc(name || "\u2014") +
        "</button>";
    });

    html += "</div>";
  });

  container.innerHTML = html;

  // Click: select meal to view recipe. Long-press / right-click: toggle swap selection.
  container.querySelectorAll(".meal-cell").forEach(function (btn) {
    btn.addEventListener("click", function (e) {
      // If shift-click or there are already swap selections, toggle swap mode
      if (e.shiftKey || Object.keys(state.swapSelections).length > 0) {
        toggleSwapSelection(btn);
        return;
      }
      var rid = btn.getAttribute("data-recipe-id");
      selectMeal(
        btn.getAttribute("data-day"),
        btn.getAttribute("data-type"),
        btn.getAttribute("data-name"),
        rid ? Number(rid) : null
      );
    });
  });

  // Swap toolbar buttons
  var swapBtn = document.getElementById("btn-swap-selected");
  if (swapBtn) swapBtn.addEventListener("click", executeSwap);
  var cancelBtn = document.getElementById("btn-swap-cancel");
  if (cancelBtn) cancelBtn.addEventListener("click", function () {
    state.swapSelections = {};
    renderMealGrid(state.mealPlan ? state.mealPlan.meals : []);
  });
}

function toggleSwapSelection(btn) {
  var day = btn.getAttribute("data-day");
  var type = btn.getAttribute("data-type").toUpperCase();
  var key = day + "_" + type;
  if (state.swapSelections[key]) {
    delete state.swapSelections[key];
  } else {
    state.swapSelections[key] = true;
  }
  renderMealGrid(state.mealPlan ? state.mealPlan.meals : []);
}

/* ---- Swap confirmation with humorous warnings ---- */
var SWAP_WARNINGS = [
  "This recipe will vanish into the culinary void. There is no ctrl+Z for dinner.",
  "Once swapped, this meal is gone forever. Like that leftover pizza you forgot in the back of the fridge.",
  "Warning: the AI might replace this with something even better. Or weirder. No promises.",
  "Are you sure? This recipe worked really hard to get here.",
  "Fun fact: swapped meals end up in a parallel universe where someone else eats them.",
  "This is a one-way street. The recipe you're about to lose will tell its friends.",
  "Swapping is permanent. Unlike your New Year's resolution to eat healthy, this one sticks.",
  "Last chance to appreciate this meal before it gets voted off the island.",
];

function showSwapConfirm(count, onConfirm) {
  var warning = SWAP_WARNINGS[Math.floor(Math.random() * SWAP_WARNINGS.length)];
  var label = count === 1 ? "this meal" : count + " meals";

  var overlay = document.createElement("div");
  overlay.className = "swap-confirm-overlay";
  overlay.innerHTML =
    '<div class="swap-confirm-card">' +
    '<div style="font-size:2rem;margin-bottom:0.75rem;">🔄</div>' +
    '<h3 style="font-size:1.1rem;font-weight:600;margin-bottom:0.5rem;">Swap ' + label + '?</h3>' +
    '<p style="font-size:0.875rem;color:var(--muted-foreground);margin-bottom:1.25rem;line-height:1.5;">' + warning + '</p>' +
    '<div style="display:flex;gap:0.75rem;justify-content:center;">' +
    '<button class="btn-swap-cancel" id="swap-confirm-cancel">Keep it</button>' +
    '<button class="btn-swap" id="swap-confirm-go">Swap anyway</button>' +
    '</div>' +
    '</div>';

  document.body.appendChild(overlay);

  document.getElementById("swap-confirm-cancel").addEventListener("click", function () {
    overlay.remove();
  });
  document.getElementById("swap-confirm-go").addEventListener("click", function () {
    overlay.remove();
    onConfirm();
  });
  // Click outside to dismiss
  overlay.addEventListener("click", function (e) {
    if (e.target === overlay) overlay.remove();
  });
}

function executeSwap() {
  var slots = Object.keys(state.swapSelections).map(function (key) {
    var parts = key.split("_");
    return { dayOfWeek: parts[0], mealType: parts[1] };
  });
  if (slots.length === 0) return;

  showSwapConfirm(slots.length, function () {
    doSwap(slots);
  });
}

function doSwap(slots) {
  state.swapping = true;
  renderMealGrid(state.mealPlan ? state.mealPlan.meals : []);

  Api.swapMeals(slots).then(function (plan) {
    state.mealPlan = plan;
    state.swapSelections = {};
    state.swapping = false;
    state.selectedMeal = null;
    renderMealGrid(plan.meals || []);
    renderRecipePanel();
  }).catch(function (err) {
    console.error("Swap failed:", err);
    state.swapping = false;
    renderMealGrid(state.mealPlan ? state.mealPlan.meals : []);
    alert("Failed to swap meals. Please try again.");
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
    /**
     * Parse instructions into steps. Tries numbered format first (e.g. "1. Do X. 2. Do Y."),
     * then newline-separated, then sentence-splitting as fallback.
     */
    function parseInstructionSteps(text) {
      if (!text) return [];
      // Try splitting on numbered steps: "1. " or "1) "
      var numbered = text.split(/(?:^|\n)\s*\d+[.)]\s+/).filter(Boolean);
      if (numbered.length > 1) return numbered;
      // Try splitting on newlines
      var lines = text.split(/\n+/).map(function (l) { return l.trim(); }).filter(Boolean);
      if (lines.length > 1) return lines;
      // Fallback: split on sentence boundaries at action verbs
      // Split on ". " followed by a capital letter (new sentence)
      var sentences = text.split(/\.\s+(?=[A-Z])/).filter(Boolean);
      if (sentences.length > 1) {
        return sentences.map(function (s) { return s.replace(/\.$/, '').trim(); });
      }
      // Last resort: return as single step
      return [text];
    }
    var steps = parseInstructionSteps(recipe.instructions);
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

  // Swap this meal button
  html += '<div style="margin-top:1.5rem;padding-top:1rem;border-top:1px solid var(--border);">';
  html += '<button class="btn-swap-single" id="btn-swap-this">Swap This Meal</button>';
  html += '<p class="hint" style="margin-top:0.25rem;">Generate a different recipe for this slot</p>';
  html += '</div>';

  html += "</div>";
  panel.innerHTML = html;

  // Bind swap button with confirmation
  var swapBtn = document.getElementById("btn-swap-this");
  if (swapBtn && meal) {
    swapBtn.addEventListener("click", function () {
      showSwapConfirm(1, function () {
        swapBtn.textContent = "Swapping...";
        swapBtn.disabled = true;
        var slots = [{ dayOfWeek: meal.day, mealType: meal.type.toUpperCase() }];
        Api.swapMeals(slots).then(function (plan) {
          state.mealPlan = plan;
          state.selectedMeal = null;
          renderMealGrid(plan.meals || []);
          renderRecipePanel();
        }).catch(function (err) {
          console.error("Swap failed:", err);
          swapBtn.textContent = "Swap This Meal";
          swapBtn.disabled = false;
          alert("Failed to swap meal. Please try again.");
        });
      });
    });
  }
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
    renderGroceryList(data || {});
  }).catch(function () {
    container.querySelector(".text-muted").textContent = "Failed to load grocery list.";
  });
}

function groceryItemKey(item) {
  return item.itemKey || [item.name || "", item.quantity || "", item.category || ""].join("|");
}

function indexGroceryItems(items, coveredItems) {
  state.groceryItemIndex = {};
  items.concat(coveredItems).forEach(function (item) {
    state.groceryItemIndex[groceryItemKey(item)] = item;
  });
}

function groupGroceryItems(items) {
  var categories = [];
  var seen = {};
  items.forEach(function (item) {
    if (!seen[item.category]) {
      seen[item.category] = true;
      categories.push(item.category);
    }
  });
  return categories;
}

function renderGroceryRow(item, sectionType) {
  var itemKey = groceryItemKey(item);
  var isSaving = !!state.pantrySaving[itemKey];
  var html = '<li><div class="grocery-row' + (sectionType === "covered" ? " grocery-row-covered" : "") + '">';
  html += '<div class="grocery-row-main">';
  html += '<div class="grocery-row-title">';
  html += '<span class="grocery-name">' + esc(item.name) + "</span>";
  if (sectionType === "covered" || item.covered) {
    html += '<span class="grocery-badge">In pantry</span>';
  }
  html += "</div>";

  if (item.inputMode === "number") {
    var quantityLabel = sectionType === "covered"
      ? (item.quantityValue === 0 ? "Need to buy: none" : "Need to buy: " + esc(item.quantity || ""))
      : esc(item.quantity || "");
    html += '<div class="grocery-qty">' + quantityLabel + "</div>";
  } else if (item.quantity) {
    html += '<div class="grocery-qty">' + esc(item.quantity) + "</div>";
  }
  html += "</div>";

  html += '<div class="grocery-have-control">';
  html += '<span class="grocery-have-label">Have</span>';

  if (item.inputMode === "number") {
    html += '<div class="grocery-have-input-group">';
    html += '<input class="text-input grocery-have-input" type="number" min="0" step="0.01"' +
      ' data-key="' + esc(itemKey) + '"' +
      ' value="' + esc(item.pantryQuantityValue != null ? item.pantryQuantityValue : "") + '"' +
      (isSaving ? " disabled" : "") +
      ' />';
    if (item.unit) {
      html += '<span class="grocery-unit-label">' + esc(item.unit) + "</span>";
    }
    html += "</div>";
  } else {
    html += '<button class="grocery-toggle' + (item.covered ? " active" : "") + '"' +
      ' data-key="' + esc(itemKey) + '"' +
      (isSaving ? " disabled" : "") +
      '>' + (item.covered ? "In pantry" : "I already have this") + "</button>";
  }

  html += "</div></div></li>";
  return html;
}

function renderGrocerySection(title, items, sectionType) {
  if (!items.length) return "";

  var categories = groupGroceryItems(items);
  var html = '<div class="grocery-section">';
  html += '<div class="grocery-section-header">';
  html += '<h3 class="grocery-section-title">' + esc(title) + "</h3>";
  html += '<p class="text-sm text-muted">' + items.length + (items.length === 1 ? " item" : " items") + "</p>";
  html += "</div>";

  categories.forEach(function (category) {
    html += '<div class="grocery-category">';
    html += '<h4 class="category-header">' + esc(category) + "</h4>";
    html += "<ul>";
    items.filter(function (item) { return item.category === category; }).forEach(function (item) {
      html += renderGroceryRow(item, sectionType);
    });
    html += "</ul></div>";
  });

  html += "</div>";
  return html;
}

function saveNumericPantryValue(item, quantity, control) {
  var itemKey = groceryItemKey(item);
  if (state.pantrySaving[itemKey]) return;
  state.pantrySaving[itemKey] = true;
  if (control) control.disabled = true;

  Api.updatePantryItem({
    name: item.name,
    canonicalName: item.canonicalName,
    quantity: quantity,
    unit: item.unit || null,
    covered: quantity > 0
  }).then(function () {
    delete state.pantrySaving[itemKey];
    loadGroceryList();
  }).catch(function (err) {
    delete state.pantrySaving[itemKey];
    console.error("Pantry quantity update failed:", err);
    loadGroceryList();
  });
}

function saveTogglePantryValue(item, covered, control) {
  var itemKey = groceryItemKey(item);
  if (state.pantrySaving[itemKey]) return;
  state.pantrySaving[itemKey] = true;
  if (control) control.disabled = true;

  Api.updatePantryItem({
    name: item.name,
    canonicalName: item.canonicalName,
    quantity: null,
    unit: null,
    covered: covered
  }).then(function () {
    delete state.pantrySaving[itemKey];
    loadGroceryList();
  }).catch(function (err) {
    delete state.pantrySaving[itemKey];
    console.error("Pantry toggle update failed:", err);
    loadGroceryList();
  });
}

function commitNumericPantryInput(input) {
  var item = state.groceryItemIndex[input.getAttribute("data-key")];
  if (!item) return;

  var rawValue = String(input.value || "").trim();
  if (!rawValue) {
    saveNumericPantryValue(item, 0, input);
    return;
  }

  var parsedValue = Number(rawValue);
  if (!isFinite(parsedValue) || parsedValue < 0) {
    input.value = item.pantryQuantityValue != null ? item.pantryQuantityValue : "";
    return;
  }

  saveNumericPantryValue(item, parsedValue, input);
}

function bindGroceryControls(container) {
  container.querySelectorAll(".grocery-have-input").forEach(function (input) {
    input.addEventListener("keydown", function (e) {
      if (e.key === "Enter") {
        e.preventDefault();
        input.blur();
      }
    });

    input.addEventListener("blur", function () {
      commitNumericPantryInput(input);
    });
  });

  container.querySelectorAll(".grocery-toggle").forEach(function (button) {
    button.addEventListener("click", function () {
      var item = state.groceryItemIndex[button.getAttribute("data-key")];
      if (!item) return;
      saveTogglePantryValue(item, !item.covered, button);
    });
  });
}

function renderGroceryList(data) {
  var container = $("#grocery-content");
  var items = data.items || [];
  var coveredItems = data.coveredItems || [];
  indexGroceryItems(items, coveredItems);

  if (!items.length && !coveredItems.length) {
    var emptyMessage = data.allCoveredByPantry
      ? "Everything for this week's plan is already in your pantry."
      : "Generate a meal plan first to see your grocery list.";
    var emptyHtml =
      '<h2 style="font-size:1.5rem;font-weight:600;" class="mb-2">Grocery List</h2>' +
      '<p class="text-sm text-muted">' + emptyMessage + "</p>";
    if (data.allCoveredByPantry && data.pantrySubtractedCount) {
      emptyHtml += '<p class="text-sm text-muted mt-2">' +
        esc(data.pantrySubtractedCount + " grocery items were adjusted using your pantry.") +
        "</p>";
    }
    container.innerHTML = emptyHtml;
    return;
  }

  var html = '<div style="animation: fadeIn 0.3s ease-out;">';
  html += '<div class="mb-6">';
  html += '<h2 style="font-size:1.5rem;font-weight:600;">Grocery List</h2>';
  html += '<p class="text-sm text-muted mt-1">' +
    items.length + (items.length === 1 ? " item left to buy" : " items left to buy") +
    ' &middot; ' + coveredItems.length + (coveredItems.length === 1 ? " item already in pantry" : " items already in pantry") +
    "</p>";
  if (data.pantrySubtractedCount) {
    html += '<p class="text-sm text-muted mt-1">' +
      esc(data.pantrySubtractedCount + " grocery items adjusted using your pantry amounts") +
      "</p>";
  }
  if (!items.length && coveredItems.length) {
    html += '<p class="text-sm text-muted mt-1">Everything you need for this plan is already covered by pantry items.</p>';
  }
  html += "</div>";

  html += renderGrocerySection("Need to Buy", items, "remaining");
  html += renderGrocerySection("Already in Pantry", coveredItems, "covered");

  if (items.length) {
    html += '<div class="grocery-footer">';
    html += '<button class="btn-primary btn-primary-full">Send Remaining Items to Instacart &rarr;</button>';
    html += '<p style="font-size:0.75rem;color:var(--muted-foreground);text-align:center;margin-top:0.5rem;">' +
      "Uses only the ingredients and quantities you still need to buy</p>";
    html += "</div>";
  }
  html += "</div>";

  container.innerHTML = html;
  bindGroceryControls(container);
}

/* =================================================================
   Preferences
   ================================================================= */
function loadPreferences() {
  Api.fetchPreferences().then(function (prefs) {
    state.selectedDiets = {};
    state.selectedCuisines = {};
    if (prefs.servingSize) state.servingSize = prefs.servingSize;
    if (prefs.dietaryRestrictions) {
      prefs.dietaryRestrictions.split(",").forEach(function (d) {
        var trimmed = d.trim();
        if (trimmed) state.selectedDiets[trimmed] = true;
      });
    }
    if (prefs.preferredCuisines) {
      prefs.preferredCuisines.split(",").forEach(function (c) {
        var trimmed = c.trim();
        if (trimmed) state.selectedCuisines[trimmed] = true;
      });
    }
    state.selectedProteins = {};
    if (prefs.preferredProteins) {
      prefs.preferredProteins.split(",").forEach(function (p) {
        var trimmed = p.trim();
        if (trimmed) state.selectedProteins[trimmed] = true;
      });
    }
    state.selectedVegetables = {};
    if (prefs.preferredVegetables) {
      prefs.preferredVegetables.split(",").forEach(function (v) {
        var trimmed = v.trim();
        if (trimmed) state.selectedVegetables[trimmed] = true;
      });
    }
    state.selectedFruits = {};
    if (prefs.preferredFruits) {
      prefs.preferredFruits.split(",").forEach(function (f) {
        var trimmed = f.trim();
        if (trimmed) state.selectedFruits[trimmed] = true;
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
  renderProteinPickers();
  renderVegetablePickers();
  renderFruitPickers();
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
  var allCuisines = CUISINE_OPTIONS.slice();
  Object.keys(state.selectedCuisines).forEach(function (c) {
    if (allCuisines.indexOf(c) === -1) allCuisines.push(c);
  });
  allCuisines.forEach(function (cuisine) {
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

  var input = document.getElementById("custom-cuisine-input");
  if (input && !input._bound) {
    input._bound = true;
    input.addEventListener("keydown", function (e) {
      if (e.key === "Enter" && input.value.trim()) {
        input.value.split(",").forEach(function (part) {
          var v = part.trim();
          if (v) state.selectedCuisines[v] = true;
        });
        input.value = "";
        renderCuisineChips();
        var cuisineStr = Object.keys(state.selectedCuisines).join(", ") || null;
        Api.updatePreferences({ preferredCuisines: cuisineStr });
      }
    });
  }
}

function renderProteinPickers() {
  var container = document.getElementById("protein-pickers");
  if (!container) return;

  var html = "";
  PROTEIN_OPTIONS.forEach(function (cat) {
    html += '<div class="staple-category">';
    html += '<div class="staple-category-header">';
    html += '<span class="staple-category-title">' + esc(cat.name) + '</span>';
    html += '<button class="staple-toggle-all" data-cat="' + esc(cat.name) + '">Toggle all</button>';
    html += '</div>';
    html += '<div class="staple-grid">';
    cat.items.forEach(function (item) {
      var on = !!state.selectedProteins[item];
      html += '<button class="staple-item' + (on ? " on" : "") + '" data-item="' + esc(item) + '">';
      html += '<span class="staple-check">' + CHECK_SVG + '</span>';
      html += '<span>' + esc(item) + '</span>';
      html += '</button>';
    });
    html += '</div></div>';
  });

  container.innerHTML = html;

  function saveProteins() {
    var proteinStr = Object.keys(state.selectedProteins).join(", ") || null;
    Api.updatePreferences({ preferredProteins: proteinStr });
  }

  container.querySelectorAll(".staple-item").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var item = btn.getAttribute("data-item");
      if (state.selectedProteins[item]) {
        delete state.selectedProteins[item];
      } else {
        state.selectedProteins[item] = true;
      }
      renderProteinPickers();
      saveProteins();
    });
  });

  container.querySelectorAll(".staple-toggle-all").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var catName = btn.getAttribute("data-cat");
      var cat = PROTEIN_OPTIONS.find(function (c) { return c.name === catName; });
      if (!cat) return;
      var allOn = cat.items.every(function (i) { return state.selectedProteins[i]; });
      cat.items.forEach(function (i) {
        if (allOn) { delete state.selectedProteins[i]; } else { state.selectedProteins[i] = true; }
      });
      renderProteinPickers();
      saveProteins();
    });
  });
}

function renderVegetablePickers() {
  var container = document.getElementById("vegetable-pickers");
  if (!container) return;

  var html = "";
  VEGETABLE_OPTIONS.forEach(function (cat) {
    html += '<div class="staple-category">';
    html += '<div class="staple-category-header">';
    html += '<span class="staple-category-title">' + esc(cat.name) + '</span>';
    html += '<button class="staple-toggle-all" data-cat="' + esc(cat.name) + '">Toggle all</button>';
    html += '</div>';
    html += '<div class="staple-grid">';
    cat.items.forEach(function (item) {
      var on = !!state.selectedVegetables[item];
      html += '<button class="staple-item' + (on ? " on" : "") + '" data-item="' + esc(item) + '">';
      html += '<span class="staple-check">' + CHECK_SVG + '</span>';
      html += '<span>' + esc(item) + '</span>';
      html += '</button>';
    });
    html += '</div></div>';
  });

  container.innerHTML = html;

  function saveVegetables() {
    var vegStr = Object.keys(state.selectedVegetables).join(", ") || null;
    Api.updatePreferences({ preferredVegetables: vegStr });
  }

  container.querySelectorAll(".staple-item").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var item = btn.getAttribute("data-item");
      if (state.selectedVegetables[item]) {
        delete state.selectedVegetables[item];
      } else {
        state.selectedVegetables[item] = true;
      }
      renderVegetablePickers();
      saveVegetables();
    });
  });

  container.querySelectorAll(".staple-toggle-all").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var catName = btn.getAttribute("data-cat");
      var cat = VEGETABLE_OPTIONS.find(function (c) { return c.name === catName; });
      if (!cat) return;
      var allOn = cat.items.every(function (i) { return state.selectedVegetables[i]; });
      cat.items.forEach(function (i) {
        if (allOn) { delete state.selectedVegetables[i]; } else { state.selectedVegetables[i] = true; }
      });
      renderVegetablePickers();
      saveVegetables();
    });
  });
}

function renderFruitPickers() {
  var container = document.getElementById("fruit-pickers");
  if (!container) return;

  var html = '<div class="staple-category">';
  html += '<div class="staple-category-header">';
  html += '<span class="staple-category-title"></span>';
  html += '<button class="staple-toggle-all" id="fruit-toggle-all">Toggle all</button>';
  html += '</div>';
  html += '<div class="staple-grid">';
  FRUIT_OPTIONS.forEach(function (item) {
    var on = !!state.selectedFruits[item];
    html += '<button class="staple-item' + (on ? " on" : "") + '" data-item="' + esc(item) + '">';
    html += '<span class="staple-check">' + CHECK_SVG + '</span>';
    html += '<span>' + esc(item) + '</span>';
    html += '</button>';
  });
  html += '</div></div>';

  container.innerHTML = html;

  function saveFruits() {
    var fruitStr = Object.keys(state.selectedFruits).join(", ") || null;
    Api.updatePreferences({ preferredFruits: fruitStr });
  }

  container.querySelectorAll(".staple-item").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var item = btn.getAttribute("data-item");
      if (state.selectedFruits[item]) {
        delete state.selectedFruits[item];
      } else {
        state.selectedFruits[item] = true;
      }
      renderFruitPickers();
      saveFruits();
    });
  });

  var toggleAllBtn = document.getElementById("fruit-toggle-all");
  if (toggleAllBtn) {
    toggleAllBtn.addEventListener("click", function () {
      var allOn = FRUIT_OPTIONS.every(function (f) { return state.selectedFruits[f]; });
      FRUIT_OPTIONS.forEach(function (f) {
        if (allOn) { delete state.selectedFruits[f]; } else { state.selectedFruits[f] = true; }
      });
      renderFruitPickers();
      saveFruits();
    });
  }
}

function commitCustomCuisineInput() {
  var input = document.getElementById("custom-cuisine-input");
  if (!input || !input.value.trim()) return false;

  input.value.split(",").forEach(function (part) {
    var value = part.trim();
    if (value) state.selectedCuisines[value] = true;
  });
  input.value = "";
  return true;
}

function buildCurrentPreferencePayload() {
  if (commitCustomCuisineInput()) {
    renderCuisineChips();
  }

  var dietStr = Object.keys(state.selectedDiets).join(", ") || null;
  var cuisineStr = Object.keys(state.selectedCuisines).join(", ") || null;

  return {
    pantryIngredients: "",
    servingSize: state.servingSize,
    dietaryRestrictions: dietStr || "",
    preferredCuisines: cuisineStr || ""
  };
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
  var payload = buildCurrentPreferencePayload();

  Api.generateMealPlan(payload).then(function (plan) {
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
      "<p>We couldn't build a complete plan for the current preferences. Please try again in a moment.</p>" +
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

  fetch("/api/auth/me", { credentials: "same-origin" })
    .then(function (res) { return res.ok ? res.json() : { loggedIn: false }; })
    .then(function (auth) {
      if (!auth.loggedIn) {
        var hasPending = sessionStorage.getItem("onboardingPayload");
        if (!hasPending) {
          window.location.href = "/";
          return;
        }
        window.location.href = "/register.html?from=onboarding";
        return;
      }

      updateAuthHeader(auth);

      if (Onboarding.resumeIfPending()) { revealApp(); return; }

      Onboarding.check().then(function (shown) {
        if (shown) { revealApp(); } else { initApp(); }
      }).catch(function () {
        initApp();
      });
    })
    .catch(function () {
      window.location.href = "/";
    });
});

function revealApp() {
  var ls = document.getElementById("loading-screen");
  if (ls) ls.style.display = "none";
  var app = document.querySelector(".app");
  if (app) app.style.display = "";
}

function initApp() {
  revealApp();
  Api.fetchMealPlan().then(function (plan) {
    state.mealPlan = plan;
    updatePlanSubtitle();
    renderMealGrid(plan.meals || []);
  }).catch(function () {
    renderMealGrid([]);
  });

  renderPreferences();
}
