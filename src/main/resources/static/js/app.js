"use strict";

/* =================================================================
   Constants
   ================================================================= */
var DAYS = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

var DAY_LABELS = {
  MONDAY: "Monday",
  TUESDAY: "Tuesday",
  WEDNESDAY: "Wednesday",
  THURSDAY: "Thursday",
  FRIDAY: "Friday",
  SATURDAY: "Saturday",
  SUNDAY: "Sunday",
};

var DIET_OPTIONS = [
  "Vegetarian",
  "Vegan",
  "Gluten-Free",
  "Dairy-Free",
  "Nut-Free",
  "Low-Carb",
  "Halal",
  "Kosher",
];

var CUISINE_OPTIONS = [
  "Italian",
  "Mexican",
  "Chinese",
  "Japanese",
  "Korean",
  "Thai",
  "Vietnamese",
  "Indian",
  "Mediterranean",
  "French",
  "American",
  "Middle Eastern",
  "Greek",
  "Caribbean",
  "Spanish",
];

var PROTEIN_OPTIONS = [
  {
    name: "Poultry",
    items: [
      "Chicken Breast",
      "Chicken Thighs",
      "Ground Chicken",
      "Turkey Breast",
      "Ground Turkey",
    ],
  },
  {
    name: "Beef & Pork",
    items: [
      "Ground Beef",
      "Beef Steak",
      "Beef Stew Meat",
      "Pork Chops",
      "Ground Pork",
      "Bacon",
      "Sausage",
    ],
  },
  { name: "Seafood", items: ["Salmon", "Shrimp", "Tilapia", "Tuna", "Cod"] },
  {
    name: "Other Protein",
    items: ["Eggs", "Tofu", "Tempeh", "Black Beans", "Chickpeas", "Lentils"],
  },
];

var VEGETABLE_OPTIONS = [
  {
    name: "Leafy Greens",
    items: ["Spinach", "Kale", "Romaine Lettuce", "Mixed Greens"],
  },
  {
    name: "Cruciferous",
    items: ["Broccoli", "Cauliflower", "Brussels Sprouts", "Cabbage"],
  },
  {
    name: "Everyday Veggies",
    items: [
      "Bell Peppers",
      "Zucchini",
      "Carrots",
      "Tomatoes",
      "Cucumber",
      "Green Beans",
      "Corn",
      "Peas",
    ],
  },
  {
    name: "Root & Starchy",
    items: ["Sweet Potatoes", "Potatoes", "Butternut Squash", "Beets"],
  },
  {
    name: "Alliums & Aromatics",
    items: ["Mushrooms", "Asparagus", "Celery", "Eggplant"],
  },
];

var FRUIT_OPTIONS = [
  "Bananas",
  "Apples",
  "Berries",
  "Oranges",
  "Grapes",
  "Strawberries",
  "Blueberries",
  "Avocado",
  "Mango",
  "Pineapple",
  "Peaches",
  "Pears",
  "Lemons",
  "Limes",
  "Watermelon",
];

var CHECK_SVG =
  '<svg width="10" height="8" viewBox="0 0 10 8" fill="none"><path d="M1 4L3.5 6.5L9 1" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';

/* =================================================================
   Application State
   ================================================================= */
var state = {
  view: "plan",
  selectedMeal: null,
  swapSelections: {}, // key: "DAY_TYPE" -> true
  swapping: false,
  mealPlan: null,
  checkedItems: {},
  groceryItemIndex: {},
  servingSize: 2,
  adjustedServings: null, // tracks user-adjusted servings for the current recipe
  selectedDiets: {},
  selectedCuisines: {},
  selectedProteins: {},
  selectedVegetables: {},
  selectedFruits: {},
  pantrySaving: {},
  generating: false,
  planHistory: [],
  planHistoryIndex: 0,
  viewingHistoricPlan: false,
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
        html +=
          '<a href="/admin.html" class="auth-link" style="font-weight:600;">Admin</a> ';
      }
      html +=
        '<span class="auth-email">' +
        esc(data.email) +
        "</span> " +
        '<a href="/logout" class="auth-link">Logout</a>';
      el.innerHTML = html;
    } else {
      el.innerHTML =
        '<a href="/login" class="auth-link">Login</a> ' +
        '<a href="/register" class="auth-link">Register</a>';
    }
  }

  if (authData) {
    render(authData);
    return;
  }

  fetch("/api/auth/me", { credentials: "same-origin" })
    .then(function (res) {
      return res.ok ? res.json() : { loggedIn: false };
    })
    .then(render)
    .catch(function () {
      el.innerHTML =
        '<a href="/login" class="auth-link">Login</a> ' +
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

function $(sel) {
  return document.querySelector(sel);
}
function $$(sel) {
  return document.querySelectorAll(sel);
}

function friendlyApiError(err, fallback) {
  var message = err && err.message ? String(err.message) : "";
  var jsonStart = message.indexOf("{");
  if (jsonStart >= 0) {
    try {
      var parsed = JSON.parse(message.slice(jsonStart));
      if (parsed && parsed.error) return parsed.error;
    } catch (_ignored) {}
  }
  if (message) return message;
  return fallback;
}

/**
 * Parse instructions into steps. Tries numbered format first (e.g. "1. Do X. 2. Do Y."),
 * then newline-separated, then sentence-splitting as fallback.
 */
function parseInstructionSteps(text) {
  if (!text) return [];
  var numbered = text.split(/(?:^|\n)\s*\d+[.)]\s+/).filter(Boolean);
  if (numbered.length > 1) return numbered;
  var lines = text
    .split(/\n+/)
    .map(function (l) {
      return l.trim();
    })
    .filter(Boolean);
  if (lines.length > 1) return lines;
  var sentences = text.split(/\.\s+(?=[A-Z])/).filter(Boolean);
  if (sentences.length > 1) {
    return sentences.map(function (s) {
      return s.replace(/\.$/, "").trim();
    });
  }
  return [text];
}

function showToast(message) {
  var existing = document.querySelector(".toast-notification");
  if (existing) existing.remove();
  var toast = document.createElement("div");
  toast.className = "toast-notification";
  toast.textContent = message;
  document.body.appendChild(toast);
  setTimeout(function () { toast.classList.add("show"); }, 10);
  setTimeout(function () {
    toast.classList.remove("show");
    setTimeout(function () { toast.remove(); }, 300);
  }, 3000);
}

function fallbackCopy(text) {
  var ta = document.createElement("textarea");
  ta.value = text;
  ta.style.position = "fixed";
  ta.style.left = "-9999px";
  document.body.appendChild(ta);
  ta.select();
  document.execCommand("copy");
  document.body.removeChild(ta);
}

function formatRecipeText(recipe) {
  var lines = [recipe.title];
  var meta = [];
  if (recipe.servings) meta.push("Serves " + recipe.servings);
  if (recipe.cookTimeMinutes) meta.push(recipe.cookTimeMinutes + " min");
  if (recipe.cuisine) meta.push(recipe.cuisine);
  if (meta.length) lines.push(meta.join(" · "));
  lines.push("");
  if (recipe.ingredients && recipe.ingredients.length) {
    lines.push("Ingredients:");
    recipe.ingredients.forEach(function (ing) {
      var t = "";
      if (ing.quantity != null) t += ing.quantity + " ";
      if (ing.unit != null) t += ing.unit + " ";
      t += ing.name;
      lines.push("- " + t.trim());
    });
    lines.push("");
  }
  if (recipe.instructions) {
    lines.push("Instructions:");
    var steps = parseInstructionSteps(recipe.instructions);
    steps.forEach(function (step, i) {
      lines.push(i + 1 + ". " + step.trim());
    });
  }
  return lines.join("\n");
}

/* =================================================================
   Export Week
   ================================================================= */
function buildWeekExportHTML(groceryData) {
  var plan = state.mealPlan;
  var slots = buildSlots(plan.meals || []);
  var weekLabel = plan.weekStartDate
    ? "Week of " + plan.weekStartDate
    : "Weekly Meal Plan";

  var css =
    'body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;' +
    "max-width:800px;margin:0 auto;padding:2rem;color:#1a1a1a}" +
    "h1{font-size:1.5rem;margin-bottom:0.25rem}" +
    ".subtitle{color:#666;font-size:0.875rem;margin-bottom:2rem}" +
    ".day-block{margin-bottom:1.5rem;page-break-inside:avoid}" +
    ".day-title{font-size:1.1rem;font-weight:700;text-transform:uppercase;" +
    "border-bottom:2px solid #222;padding-bottom:0.25rem;margin-bottom:0.5rem}" +
    ".meal-row{display:flex;padding:0.25rem 0}" +
    ".meal-type{width:80px;font-weight:600;color:#555;flex-shrink:0}" +
    ".meal-name{flex:1}" +
    ".grocery-section{margin-top:2rem;page-break-before:always}" +
    ".grocery-heading{font-size:1.25rem;font-weight:700;border-bottom:2px solid #222;" +
    "padding-bottom:0.25rem;margin-bottom:1rem}" +
    ".cat-title{font-weight:600;margin-top:1rem;margin-bottom:0.25rem}" +
    ".g-item{padding:0.15rem 0 0.15rem 1.5rem}" +
    ".cb{display:inline-block;width:12px;height:12px;border:1px solid #666;" +
    "margin-right:0.5rem;vertical-align:middle}" +
    "@media print{body{font-size:11pt;padding:0}}";

  var html =
    '<!DOCTYPE html><html><head><meta charset="UTF-8">' +
    "<title>" +
    esc(weekLabel) +
    " \u2013 SmartCart</title>" +
    "<style>" +
    css +
    "</style></head><body>";

  html += "<h1>" + esc(weekLabel) + "</h1>";
  html += '<p class="subtitle">Generated by SmartCart</p>';

  slots.forEach(function (slot) {
    html += '<div class="day-block">';
    html +=
      '<div class="day-title">' +
      esc(DAY_LABELS[slot.day] || slot.day) +
      "</div>";
    [
      ["Breakfast", slot.breakfast],
      ["Lunch", slot.lunch],
      ["Dinner", slot.dinner],
    ].forEach(function (pair) {
      html +=
        '<div class="meal-row">' +
        '<span class="meal-type">' +
        pair[0] +
        "</span>" +
        '<span class="meal-name">' +
        esc(pair[1] || "\u2014") +
        "</span>" +
        "</div>";
    });
    html += "</div>";
  });

  if (groceryData) {
    var items = groceryData.items || [];
    var coveredItems = groceryData.coveredItems || [];

    if (items.length || coveredItems.length) {
      html += '<div class="grocery-section">';
      html += '<div class="grocery-heading">Grocery List</div>';

      if (items.length) {
        html += buildGroceryCategoryHTML(items, "Need to Buy");
      }
      if (coveredItems.length) {
        html += buildGroceryCategoryHTML(coveredItems, "Already in Pantry");
      }

      html += "</div>";
    }
  }

  html += "</body></html>";
  return html;
}

function buildGroceryCategoryHTML(items, sectionTitle) {
  var categories = {};
  var catOrder = [];
  items.forEach(function (item) {
    var cat = item.category || "Other";
    if (!categories[cat]) {
      categories[cat] = [];
      catOrder.push(cat);
    }
    categories[cat].push(item);
  });

  var html =
    '<p style="font-weight:600;margin-top:1rem;margin-bottom:0.25rem;font-size:1rem;">' +
    esc(sectionTitle) +
    "</p>";
  catOrder.forEach(function (cat) {
    html += '<div class="cat-title">' + esc(cat) + "</div>";
    categories[cat].forEach(function (item) {
      var text = "";
      if (
        sectionTitle === "Already in Pantry" &&
        item.pantryQuantityValue != null
      ) {
        text += item.pantryQuantityValue + " ";
        if (item.unit) text += item.unit + " ";
      } else if (item.quantity) {
        text += item.quantity + " ";
      }
      text += item.name || "";
      var checkbox = sectionTitle === "Already in Pantry" ? "" : '<span class="cb"></span>';
      html +=
        '<div class="g-item">' + checkbox +
        esc(text.trim()) +
        "</div>";
    });
  });
  return html;
}

function handleExportWeek() {
  if (
    !state.mealPlan ||
    !state.mealPlan.meals ||
    !state.mealPlan.meals.length
  ) {
    showToast("Generate a meal plan first before exporting.");
    return;
  }
  var btn = document.getElementById("btn-export-week");
  if (btn) {
    btn.textContent = "Exporting\u2026";
    btn.disabled = true;
  }

  Api.fetchGroceryList()
    .then(function (groceryData) {
      return groceryData;
    })
    .catch(function () {
      return null;
    })
    .then(function (groceryData) {
      var html = buildWeekExportHTML(groceryData);
      var w = window.open("");
      if (w) {
        w.document.write(html);
        w.document.close();
      }
      if (btn) {
        btn.textContent = "Export Week";
        btn.disabled = false;
      }
    });
}

/* =================================================================
   View Switching
   ================================================================= */
function switchView(view) {
  state.view = view;

  var views = {
    plan: "plan-view",
    grocery: "grocery-view",
    favourites: "favourites-view",
    preferences: "preferences-view",
  };
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
  if (view === "favourites") loadFavourites();
  if (view === "preferences") loadPreferences();
}

/* =================================================================
   Meal Plan History Navigation
   ================================================================= */
function loadPlanHistory() {
  Api.getMealPlanHistory(0, 52).then(function (data) {
    state.planHistory = data.items || [];
    state.planHistoryIndex = 0;
    state.viewingHistoricPlan = false;
    renderPlanNav();
  }).catch(function () { state.planHistory = []; renderPlanNav(); });
}
function renderPlanNav() {
  var nav = document.getElementById("plan-nav");
  if (!nav) return;
  var history = state.planHistory, idx = state.planHistoryIndex;
  if (!history.length) { nav.innerHTML = ""; return; }
  var isLatest = idx === 0, isOldest = idx >= history.length - 1;
  var current = history[idx];
  var label = isLatest ? "This Week" : "Week of " + current.weekStartDate;
  var html = '<div class="plan-nav">';
  html += '<button class="plan-nav-btn" id="btn-plan-prev"' + (isOldest ? " disabled" : "") + '>&lsaquo; Previous Week</button>';
  html += '<span class="plan-nav-label">' + esc(label) + '</span>';
  html += '<button class="plan-nav-btn" id="btn-plan-next"' + (isLatest ? " disabled" : "") + '>Next Week &rsaquo;</button>';
  html += '</div>';
  nav.innerHTML = html;
  var prevBtn = document.getElementById("btn-plan-prev");
  var nextBtn = document.getElementById("btn-plan-next");
  if (prevBtn) prevBtn.addEventListener("click", function () {
    if (state.planHistoryIndex < state.planHistory.length - 1) { state.planHistoryIndex++; navigateToPlan(state.planHistoryIndex); }
  });
  if (nextBtn) nextBtn.addEventListener("click", function () {
    if (state.planHistoryIndex > 0) { state.planHistoryIndex--; navigateToPlan(state.planHistoryIndex); }
  });
}
function navigateToPlan(idx) {
  var entry = state.planHistory[idx];
  if (!entry) return;
  state.viewingHistoricPlan = idx !== 0;
  Api.getMealPlanById(entry.id).then(function (plan) {
    state.mealPlan = plan; state.selectedMeal = null; state.swapSelections = {};
    updatePlanSubtitle(); renderPlanNav(); renderMealGrid(plan.meals || []);
    renderRecipePanel(); updateHistoricPlanUI();
  }).catch(function () { renderPlanNav(); });
}
function updateHistoricPlanUI() {
  var grid = document.getElementById("meal-grid");
  var generateBtn = document.getElementById("generate-btn");
  if (state.viewingHistoricPlan) {
    if (grid) grid.classList.add("plan-readonly");
    if (generateBtn) generateBtn.style.display = "none";
  } else {
    if (grid) grid.classList.remove("plan-readonly");
    if (generateBtn) generateBtn.style.display = "";
  }
}

/* =================================================================
   Meal Plan Grid
   ================================================================= */
function buildSlots(meals) {
  var byDay = {};
  DAYS.forEach(function (d) {
    byDay[d] = {
      day: d,
      breakfast: null,
      breakfastId: null,
      lunch: null,
      lunchId: null,
      dinner: null,
      dinnerId: null,
    };
  });
  if (meals) {
    meals.forEach(function (m) {
      var slot = byDay[m.dayOfWeek];
      if (!slot) return;
      if (m.mealType === "BREAKFAST") {
        slot.breakfast = m.recipeName;
        slot.breakfastId = m.recipeId;
      }
      if (m.mealType === "LUNCH") {
        slot.lunch = m.recipeName;
        slot.lunchId = m.recipeId;
      }
      if (m.mealType === "DINNER") {
        slot.dinner = m.recipeName;
        slot.dinnerId = m.recipeId;
      }
    });
  }
  return DAYS.map(function (d) {
    return byDay[d];
  });
}

function renderMealGrid(meals) {
  var container = $("#meal-grid");
  var slots = buildSlots(meals);

  // Swap toolbar
  var swapCount = Object.keys(state.swapSelections).length;
  var toolbarHtml =
    '<div class="swap-toolbar" id="swap-toolbar"' +
    (swapCount > 0 ? "" : ' style="display:none"') +
    ">" +
    "<span>" +
    swapCount +
    " meal" +
    (swapCount !== 1 ? "s" : "") +
    " selected</span>" +
    '<button class="btn-swap" id="btn-swap-selected"' +
    (state.swapping ? " disabled" : "") +
    ">" +
    (state.swapping ? "Swapping..." : "Swap Selected") +
    "</button>" +
    '<button class="btn-swap-cancel" id="btn-swap-cancel">Clear</button>' +
    "</div>";

  var html = toolbarHtml;
  html +=
    '<div class="grid-header">' +
    "<span>Day</span><span>Breakfast</span><span>Lunch</span><span>Dinner</span>" +
    "</div>";

  slots.forEach(function (slot) {
    html += '<div class="grid-row">';
    html +=
      '<div class="day-label">' +
      esc(DAY_LABELS[slot.day] || slot.day) +
      "</div>";

    ["breakfast", "lunch", "dinner"].forEach(function (type) {
      var name = slot[type];
      var recipeId = slot[type + "Id"];
      var sel =
        state.selectedMeal &&
        state.selectedMeal.day === slot.day &&
        state.selectedMeal.type === type;
      var swapKey = slot.day + "_" + type.toUpperCase();
      var swapChecked = !!state.swapSelections[swapKey];
      html +=
        '<button class="meal-cell' +
        (sel ? " selected" : "") +
        (swapChecked ? " swap-checked" : "") +
        (state.swapping ? " swapping" : "") +
        '"' +
        ' data-day="' +
        esc(slot.day) +
        '"' +
        ' data-type="' +
        esc(type) +
        '"' +
        ' data-name="' +
        esc(name || "\u2014") +
        '"' +
        ' data-recipe-id="' +
        (recipeId != null ? recipeId : "") +
        '">' +
        (swapChecked ? '<span class="swap-check-icon">&#10003;</span>' : "") +
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
        rid ? Number(rid) : null,
      );
    });
  });

  // Swap toolbar buttons
  var swapBtn = document.getElementById("btn-swap-selected");
  if (swapBtn) swapBtn.addEventListener("click", executeSwap);
  var cancelBtn = document.getElementById("btn-swap-cancel");
  if (cancelBtn)
    cancelBtn.addEventListener("click", function () {
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
    '<h3 style="font-size:1.1rem;font-weight:600;margin-bottom:0.5rem;">Swap ' +
    label +
    "?</h3>" +
    '<p style="font-size:0.875rem;color:var(--muted-foreground);margin-bottom:1.25rem;line-height:1.5;">' +
    warning +
    "</p>" +
    '<div style="display:flex;gap:0.75rem;justify-content:center;">' +
    '<button class="btn-swap-cancel" id="swap-confirm-cancel">Keep it</button>' +
    '<button class="btn-swap" id="swap-confirm-go">Swap anyway</button>' +
    "</div>" +
    "</div>";

  document.body.appendChild(overlay);

  document
    .getElementById("swap-confirm-cancel")
    .addEventListener("click", function () {
      overlay.remove();
    });
  document
    .getElementById("swap-confirm-go")
    .addEventListener("click", function () {
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

  Api.swapMeals(slots)
    .then(function (plan) {
      state.mealPlan = plan;
      state.swapSelections = {};
      state.swapping = false;
      state.selectedMeal = null;
      renderMealGrid(plan.meals || []);
      renderRecipePanel();
    })
    .catch(function (err) {
      console.error("Swap failed:", err);
      state.swapping = false;
      renderMealGrid(state.mealPlan ? state.mealPlan.meals : []);
      showToast("Failed to swap meals. Please try again.");
    });
}

function selectMeal(day, type, name, recipeId) {
  state.selectedMeal = { day: day, type: type, name: name, recipeId: recipeId };
  state.adjustedServings = null; // reset so it defaults to the recipe's base servings

  $$(".meal-cell").forEach(function (c) {
    if (
      c.getAttribute("data-day") === day &&
      c.getAttribute("data-type") === type
    ) {
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
    '<p class="section-label mb-1">' +
    esc(dayLabel) +
    " &middot; " +
    esc(meal.type) +
    "</p>" +
    '<h2 style="font-size:1.5rem;font-weight:600;" class="mb-6">' +
    esc(meal.name) +
    "</h2>" +
    '<p class="text-muted">Loading recipe&hellip;</p>' +
    "</div>";

  if (meal.recipeId == null) {
    panel.querySelector(".text-muted").innerHTML =
      '<span class="font-serif" style="font-style:italic;">Recipe details for &ldquo;' +
      esc(meal.name) +
      "&rdquo; will appear here once generated.</span>";
    return;
  }

  Api.fetchRecipe(meal.recipeId)
    .then(function (recipe) {
      if (state.selectedMeal && state.selectedMeal.recipeId !== recipe.id)
        return;
      renderRecipeDetail(recipe);
    })
    .catch(function () {
      panel.querySelector(".text-muted").textContent = "Failed to load recipe.";
    });
}

function renderRecipeDetail(recipe) {
  var panel = $("#recipe-panel");
  var meal = state.selectedMeal;
  var dayLabel = DAY_LABELS[meal.day] || meal.day;

  var html = '<div class="recipe-detail">';
  html +=
    '<p class="section-label mb-1">' +
    esc(dayLabel) +
    " &middot; " +
    esc(meal.type) +
    "</p>";
  html +=
    '<div style="display:flex;align-items:center;gap:0.5rem;" class="mb-6">' +
    '<h2 style="font-size:1.5rem;font-weight:600;">' +
    esc(recipe.title) +
    "</h2>" +
    '<button class="favourite-btn" id="btn-fav-recipe" title="Save to favourites">' +
    "\u2661" +
    "</button>" +
    "</div>";

  var baseServings = recipe.servings || 1;
  var currentServings = state.adjustedServings || baseServings;
  state.adjustedServings = currentServings;

  var metaParts = [];
  if (recipe.servings) metaParts.push("Serves " + recipe.servings);
  if (recipe.cookTimeMinutes)
    metaParts.push("Cook: " + recipe.cookTimeMinutes + " min");
  if (recipe.cuisine) metaParts.push(esc(recipe.cuisine));
  if (metaParts.length) {
    html +=
      '<div class="recipe-meta">' +
      metaParts
        .map(function (s) {
          return "<span>" + s + "</span>";
        })
        .join("") +
      "</div>";
  }

  // Serving adjuster
  html += '<div class="serving-adjuster">';
  html += '<span class="section-label">Serves:</span>';
  html +=
    '<button class="serving-btn" id="btn-serving-minus" ' +
    (currentServings <= 1 ? "disabled" : "") +
    ">&minus;</button>";
  html += '<span class="serving-count" id="serving-count">' + currentServings + "</span>";
  html +=
    '<button class="serving-btn" id="btn-serving-plus" ' +
    (currentServings >= 8 ? "disabled" : "") +
    ">&plus;</button>";
  html += '<span class="text-sm text-muted">(per serving)</span>';
  html += "</div>";

  // Nutrition bar (scaled by serving ratio)
  var servingRatio = currentServings / baseServings;
  if (recipe.nutrition && recipe.nutrition.totalCalories > 0) {
    var n = recipe.nutrition;
    var scaledCal = Math.round(n.totalCalories * servingRatio);
    var scaledProt = (n.totalProteinG * servingRatio).toFixed(1);
    var scaledCarb = (n.totalCarbsG * servingRatio).toFixed(1);
    var scaledFat = (n.totalFatG * servingRatio).toFixed(1);
    html += '<div class="nutrition-bar">';
    html += '<div class="nutrition-item"><span class="nutrition-value">' + scaledCal + '</span><span class="nutrition-label">cal</span></div>';
    html += '<div class="nutrition-item"><span class="nutrition-value">' + scaledProt + 'g</span><span class="nutrition-label">protein</span></div>';
    html += '<div class="nutrition-item"><span class="nutrition-value">' + scaledCarb + 'g</span><span class="nutrition-label">carbs</span></div>';
    html += '<div class="nutrition-item"><span class="nutrition-value">' + scaledFat + 'g</span><span class="nutrition-label">fat</span></div>';
    html += '</div>';
  }

  if (recipe.estimatedCost > 0) {
    var scaledCost = (recipe.estimatedCost * servingRatio).toFixed(2);
    html += '<div class="recipe-cost">Est. cost: $' + scaledCost + '</div>';
  }

  if (recipe.ingredients && recipe.ingredients.length) {
    var ratio = currentServings / baseServings;
    html += '<div class="mb-8">';
    html += '<h3 class="section-label mb-3">Ingredients</h3>';
    html += '<ul class="ingredient-list" id="ingredient-list">';
    recipe.ingredients.forEach(function (ing) {
      var text = "";
      if (ing.quantity != null) {
        var adjusted = Math.round(ing.quantity * ratio * 10) / 10;
        text += adjusted + " ";
      }
      if (ing.unit != null) text += ing.unit + " ";
      text += ing.name;
      html +=
        '<li class="ingredient-item">' +
        '<span class="ingredient-bullet"></span>' +
        esc(text) +
        "</li>";
    });
    html += "</ul></div>";
  }

  if (recipe.instructions) {
    var steps = parseInstructionSteps(recipe.instructions);
    html += "<div>";
    html += '<h3 class="section-label mb-3">Instructions</h3>';
    html += '<ol class="instruction-list">';
    steps.forEach(function (step, i) {
      html +=
        '<li class="instruction-step">' +
        '<span class="step-number">' +
        (i + 1) +
        "</span>" +
        "<span>" +
        esc(step.trim()) +
        "</span>" +
        "</li>";
    });
    html += "</ol></div>";
  }

  html +=
    '<div class="recipe-actions" style="display:flex;gap:0.5rem;margin-top:1.5rem;">';
  html += '<button class="btn-action" id="btn-print-recipe">Print</button>';
  html += '<button class="btn-action" id="btn-copy-recipe">Copy</button>';
  html += "</div>";

  if (!state.viewingHistoricPlan) {
    html +=
      '<div style="margin-top:1.5rem;padding-top:1rem;border-top:1px solid var(--border);">';
    html +=
      '<button class="btn-swap-single" id="btn-swap-this">Swap This Meal</button>';
    html +=
      '<p class="hint" style="margin-top:0.25rem;">Generate a different recipe for this slot</p>';
    html += "</div>";
  }

  html += "</div>";
  panel.innerHTML = html;

  var printBtn = document.getElementById("btn-print-recipe");
  if (printBtn) {
    printBtn.addEventListener("click", function () {
      window.print();
    });
  }

  var copyBtn = document.getElementById("btn-copy-recipe");
  if (copyBtn) {
    copyBtn.addEventListener("click", function () {
      var text = formatRecipeText(recipe);
      function showCopied() {
        copyBtn.textContent = "Copied!";
        setTimeout(function () { copyBtn.textContent = "Copy"; }, 1500);
      }
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(showCopied).catch(function () {
          fallbackCopy(text);
          showCopied();
        });
      } else {
        fallbackCopy(text);
        showCopied();
      }
    });
  }

  var minusBtn = document.getElementById("btn-serving-minus");
  var plusBtn = document.getElementById("btn-serving-plus");
  if (minusBtn) {
    minusBtn.addEventListener("click", function () {
      if (state.adjustedServings > 1) {
        state.adjustedServings--;
        renderRecipeDetail(recipe);
      }
    });
  }
  if (plusBtn) {
    plusBtn.addEventListener("click", function () {
      if (state.adjustedServings < 8) {
        state.adjustedServings++;
        renderRecipeDetail(recipe);
      }
    });
  }

  var swapBtn = document.getElementById("btn-swap-this");
  if (swapBtn && meal) {
    swapBtn.addEventListener("click", function () {
      showSwapConfirm(1, function () {
        swapBtn.textContent = "Swapping...";
        swapBtn.disabled = true;
        var slots = [
          { dayOfWeek: meal.day, mealType: meal.type.toUpperCase() },
        ];
        Api.swapMeals(slots)
          .then(function (plan) {
            state.mealPlan = plan;
            state.selectedMeal = null;
            renderMealGrid(plan.meals || []);
            renderRecipePanel();
          })
          .catch(function (err) {
            console.error("Swap failed:", err);
            swapBtn.textContent = "Swap This Meal";
            swapBtn.disabled = false;
            showToast("Failed to swap meal. Please try again.");
          });
      });
    });
  }

  // Favourite button
  var favBtn = document.getElementById("btn-fav-recipe");
  if (favBtn && recipe.id) {
    Api.isFavourite(recipe.id).then(function (data) {
      if (data.favourited) {
        favBtn.textContent = "\u2665";
        favBtn.classList.add("active");
      } else {
        favBtn.textContent = "\u2661";
        favBtn.classList.remove("active");
      }
    }).catch(function () {});

    favBtn.addEventListener("click", function () {
      favBtn.disabled = true;
      Api.toggleFavourite(recipe.id).then(function (data) {
        favBtn.disabled = false;
        if (data.favourited) {
          favBtn.textContent = "\u2665";
          favBtn.classList.add("active");
        } else {
          favBtn.textContent = "\u2661";
          favBtn.classList.remove("active");
        }
      }).catch(function () {
        favBtn.disabled = false;
      });
    });
  }
}

/* =================================================================
   Favourites
   ================================================================= */
function loadFavourites() {
  var container = document.getElementById("favourites-list");
  if (!container) return;
  container.innerHTML = '<p class="text-sm text-muted">Loading favourites&hellip;</p>';

  Api.getFavourites()
    .then(function (data) {
      renderFavourites(data.favourites || []);
    })
    .catch(function () {
      container.innerHTML = '<p class="text-sm text-muted">Failed to load favourites.</p>';
    });
}

function renderFavourites(favourites) {
  var container = document.getElementById("favourites-list");
  var subtitle = document.getElementById("favourites-subtitle");

  if (!favourites.length) {
    container.innerHTML =
      '<div class="empty-state" style="padding:2rem 0;">' +
      "<p>No favourite recipes yet</p>" +
      "<p>Click the heart icon on any recipe to save it here</p>" +
      "</div>";
    if (subtitle) subtitle.textContent = "Your saved recipes";
    return;
  }

  if (subtitle) {
    subtitle.textContent = favourites.length + (favourites.length === 1 ? " recipe saved" : " recipes saved");
  }

  var html = '<div class="favourites-grid">';
  favourites.forEach(function (fav) {
    html += '<div class="favourite-card" data-recipe-id="' + fav.recipeId + '">';
    html += '<div class="favourite-card-header">';
    html += '<span class="favourite-card-title">' + esc(fav.recipeTitle) + "</span>";
    html += '<button class="favourite-btn active favourite-remove" data-recipe-id="' + fav.recipeId + '" title="Remove from favourites">\u2665</button>';
    html += "</div>";
    var meta = [];
    if (fav.cuisine) meta.push(esc(fav.cuisine));
    if (fav.cookTimeMinutes) meta.push(fav.cookTimeMinutes + " min");
    if (meta.length) {
      html += '<p class="text-sm text-muted">' + meta.join(" &middot; ") + "</p>";
    }
    html += "</div>";
  });
  html += "</div>";

  container.innerHTML = html;

  container.querySelectorAll(".favourite-card").forEach(function (card) {
    card.addEventListener("click", function (e) {
      if (e.target.closest(".favourite-remove")) return;
      var recipeId = Number(card.getAttribute("data-recipe-id"));
      Api.fetchRecipe(recipeId).then(function (recipe) {
        state.selectedMeal = {
          day: "",
          type: "Favourite",
          name: recipe.title,
          recipeId: recipe.id,
        };
        switchView("plan");
        renderRecipeDetail(recipe);
      }).catch(function () {
        showToast("Failed to load recipe.");
      });
    });
  });

  container.querySelectorAll(".favourite-remove").forEach(function (btn) {
    btn.addEventListener("click", function (e) {
      e.stopPropagation();
      var recipeId = Number(btn.getAttribute("data-recipe-id"));
      btn.disabled = true;
      Api.toggleFavourite(recipeId).then(function () {
        loadFavourites();
      }).catch(function () {
        btn.disabled = false;
      });
    });
  });
}

/* =================================================================
   Grocery List
   ================================================================= */
function loadGroceryList() {
  var container = $("#grocery-content");
  container.innerHTML =
    '<h2 style="font-size:1.5rem;font-weight:600;" class="mb-2">Grocery List</h2>' +
    '<p class="text-sm text-muted">Loading grocery list&hellip;</p>';

  Api.fetchGroceryList()
    .then(function (data) {
      renderGroceryList(data || {});
    })
    .catch(function () {
      container.querySelector(".text-muted").textContent =
        "Failed to load grocery list.";
    });
}

function groceryItemKey(item) {
  return (
    item.itemKey ||
    [item.name || "", item.quantity || "", item.category || ""].join("|")
  );
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
  var html =
    '<li><div class="grocery-row' +
    (sectionType === "covered" ? " grocery-row-covered" : "") +
    '">';
  html += '<div class="grocery-row-main">';
  html += '<div class="grocery-row-title">';
  html += '<span class="grocery-name">' + esc(item.name) + "</span>";
  if (sectionType === "covered" || item.covered) {
    html += '<span class="grocery-badge">In pantry</span>';
  }
  html += "</div>";

  if (item.inputMode === "number") {
    var storeQty = item.storeQuantity || item.quantity || "";
    var quantityLabel =
      sectionType === "covered"
        ? item.quantityValue === 0
          ? "Need to buy: none"
          : "Need to buy: " + esc(storeQty)
        : esc(storeQty);
    html += '<div class="grocery-qty">' + quantityLabel + "</div>";
  } else if (item.storeQuantity || item.quantity) {
    html += '<div class="grocery-qty">' + esc(item.storeQuantity || item.quantity) + "</div>";
  }
  html += "</div>";

  html += '<div class="grocery-have-control">';
  html += '<span class="grocery-have-label">Have</span>';

  if (item.inputMode === "number") {
    html += '<div class="grocery-have-input-group">';
    html +=
      '<input class="text-input grocery-have-input" type="number" min="0" step="0.01"' +
      ' data-key="' +
      esc(itemKey) +
      '"' +
      ' value="' +
      esc(item.pantryQuantityValue != null ? item.pantryQuantityValue : "") +
      '"' +
      (isSaving ? " disabled" : "") +
      " />";
    if (item.unit) {
      html += '<span class="grocery-unit-label">' + esc(item.unit) + "</span>";
    }
    html += "</div>";
  } else {
    html +=
      '<button class="grocery-toggle' +
      (item.covered ? " active" : "") +
      '"' +
      ' data-key="' +
      esc(itemKey) +
      '"' +
      (isSaving ? " disabled" : "") +
      ">" +
      (item.covered ? "In pantry" : "I already have this") +
      "</button>";
  }

  // Swap button (only for "Need to Buy" items, not pantry)
  if (sectionType === "remaining") {
    html += '<button class="grocery-swap-btn" data-name="' + esc(item.canonicalName || item.name) + '" data-display="' + esc(item.name) + '">Swap</button>';
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
  html +=
    '<p class="text-sm text-muted">' +
    items.length +
    (items.length === 1 ? " item" : " items") +
    "</p>";
  html += "</div>";

  categories.forEach(function (category) {
    html += '<div class="grocery-category">';
    html += '<h4 class="category-header">' + esc(category) + "</h4>";
    html += "<ul>";
    items
      .filter(function (item) {
        return item.category === category;
      })
      .forEach(function (item) {
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
    covered: quantity > 0,
  })
    .then(function () {
      delete state.pantrySaving[itemKey];
      loadGroceryList();
    })
    .catch(function (err) {
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
    covered: covered,
  })
    .then(function () {
      delete state.pantrySaving[itemKey];
      loadGroceryList();
    })
    .catch(function (err) {
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
    input.value =
      item.pantryQuantityValue != null ? item.pantryQuantityValue : "";
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

  var instacartButton = container.querySelector("#btn-instacart");
  if (instacartButton) {
    instacartButton.addEventListener("click", handleInstacartCheckout);
  }

  bindSwapButtons(container);

  var copyGroceryBtn = container.querySelector("#btn-copy-grocery");
  if (copyGroceryBtn) {
    copyGroceryBtn.addEventListener("click", function () {
      var text = buildShoppingListText();
      function showCopied() {
        copyGroceryBtn.textContent = "Copied!";
        setTimeout(function () { copyGroceryBtn.textContent = "Copy Shopping List"; }, 1500);
      }
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(showCopied).catch(function () {
          fallbackCopy(text);
          showCopied();
        });
      } else {
        fallbackCopy(text);
        showCopied();
      }
    });
  }

  var printGroceryBtn = container.querySelector("#btn-print-grocery");
  if (printGroceryBtn) {
    printGroceryBtn.addEventListener("click", function () {
      var text = buildShoppingListText();
      var w = window.open("", "_blank");
      w.document.write('<html><head><title>Shopping List</title>');
      w.document.write('<style>body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;max-width:500px;margin:2rem auto;font-size:14px;}');
      w.document.write('h1{font-size:1.5rem;margin-bottom:1rem;} h2{font-size:1.1rem;margin:1.5rem 0 0.5rem;color:#555;text-transform:uppercase;letter-spacing:0.05em;border-bottom:1px solid #ddd;padding-bottom:0.25rem;}');
      w.document.write('.item{padding:0.3rem 0;display:flex;gap:0.5rem;} .check{width:14px;height:14px;border:1.5px solid #999;border-radius:2px;flex-shrink:0;margin-top:2px;}');
      w.document.write('</style></head><body>');
      w.document.write('<h1>Shopping List</h1>');
      var items = state.lastGroceryData ? state.lastGroceryData.items || [] : [];
      var byCategory = groupByCategory(items);
      Object.keys(byCategory).forEach(function (cat) {
        w.document.write('<h2>' + esc(cat) + '</h2>');
        byCategory[cat].forEach(function (item) {
          var qty = item.storeQuantity || (item.quantity || "") + " " + (item.unit || "");
          w.document.write('<div class="item"><div class="check"></div><span>' + esc(item.name) + ' — ' + esc(qty.trim()) + '</span></div>');
        });
      });
      w.document.write('</body></html>');
      w.document.close();
      w.print();
    });
  }
}

function handleInstacartCheckout(event) {
  if (event) {
    event.preventDefault();
  }
}

function bindSwapButtons(container) {
  container.querySelectorAll(".grocery-swap-btn").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var ingredientName = btn.getAttribute("data-name");
      var displayName = btn.getAttribute("data-display");
      btn.textContent = "Loading...";
      btn.disabled = true;
      fetch("/api/grocery-list/swap-options?ingredient=" + encodeURIComponent(ingredientName), { credentials: "same-origin" })
        .then(function (res) { return res.json(); })
        .then(function (data) {
          btn.textContent = "Swap";
          btn.disabled = false;
          if (!data.alternatives || data.alternatives.length === 0) {
            showSwapDropdown(btn, displayName, [{ name: "No alternatives found", note: "", priceCad: null }]);
          } else {
            showSwapDropdown(btn, displayName, data.alternatives);
          }
        })
        .catch(function () {
          btn.textContent = "Swap";
          btn.disabled = false;
        });
    });
  });
}

// Track the current outside-click handler so it can be cleaned up when a new
// dropdown opens (prevents orphaned listeners when re-clicking the same button).
var _swapDropdownCleanup = null;

function showSwapDropdown(anchorBtn, currentName, alternatives) {
  // Clean up any previous outside-click listener before removing the old dropdown
  if (_swapDropdownCleanup) {
    _swapDropdownCleanup();
    _swapDropdownCleanup = null;
  }

  // Remove any existing dropdown
  var existing = document.querySelector(".swap-dropdown");
  if (existing) existing.remove();

  var dropdown = document.createElement("div");
  dropdown.className = "swap-dropdown";

  var header = document.createElement("div");
  header.className = "swap-dropdown-header";
  header.textContent = "Swap " + currentName + " for:";
  dropdown.appendChild(header);

  alternatives.forEach(function (alt) {
    if (!alt.priceCad && alt.name === "No alternatives found") {
      var empty = document.createElement("div");
      empty.className = "swap-dropdown-item";
      empty.textContent = "No alternatives available for this item";
      empty.style.color = "var(--muted-foreground)";
      dropdown.appendChild(empty);
      return;
    }
    var item = document.createElement("div");
    item.className = "swap-dropdown-item";

    var nameSpan = document.createElement("span");
    nameSpan.className = "swap-item-name";
    nameSpan.textContent = alt.name;
    item.appendChild(nameSpan);

    var details = document.createElement("span");
    details.className = "swap-item-details";
    var parts = [];
    if (alt.priceCad) parts.push("$" + alt.priceCad.toFixed(2) + "/" + alt.priceUnit);
    if (alt.caloriesPer100g) parts.push(alt.caloriesPer100g + " cal/100g");
    if (alt.note && alt.note !== "Same category") parts.push(alt.note);
    details.textContent = parts.join(" · ");
    item.appendChild(details);

    item.addEventListener("click", function () {
      dropdown.remove();
      if (_swapDropdownCleanup) {
        _swapDropdownCleanup();
        _swapDropdownCleanup = null;
      }
      // Visually update the grocery item to show the swap
      var groceryRow = anchorBtn.closest("li");
      if (groceryRow) {
        var nameEl = groceryRow.querySelector(".grocery-name");
        if (nameEl) {
          nameEl.textContent = alt.name;
          nameEl.style.color = "var(--primary)";
        }
        var swapBtn = groceryRow.querySelector(".grocery-swap-btn");
        if (swapBtn) {
          swapBtn.textContent = "Swapped";
          swapBtn.disabled = true;
          swapBtn.style.background = "var(--celery-light)";
          swapBtn.style.color = "var(--primary)";
          swapBtn.style.borderColor = "var(--primary)";
        }
      }
      showToast("Swapped " + currentName + " for " + alt.name);
    });

    dropdown.appendChild(item);
  });

  // Position near the button
  anchorBtn.style.position = "relative";
  anchorBtn.parentElement.appendChild(dropdown);

  // Close on click outside
  setTimeout(function () {
    function closeDropdown(e) {
      if (!dropdown.contains(e.target) && e.target !== anchorBtn) {
        dropdown.remove();
        document.removeEventListener("click", closeDropdown);
        _swapDropdownCleanup = null;
      }
    }
    document.addEventListener("click", closeDropdown);
    _swapDropdownCleanup = function () {
      document.removeEventListener("click", closeDropdown);
    };
  }, 10);
}

function groupByCategory(items) {
  var cats = {};
  items.forEach(function (item) {
    var cat = item.category || "Other";
    if (!cats[cat]) cats[cat] = [];
    cats[cat].push(item);
  });
  return cats;
}

function buildShoppingListText() {
  var items = state.lastGroceryData ? state.lastGroceryData.items || [] : [];
  var byCategory = groupByCategory(items);
  var lines = ["SHOPPING LIST", ""];
  Object.keys(byCategory).forEach(function (cat) {
    lines.push(cat.toUpperCase());
    byCategory[cat].forEach(function (item) {
      var qty = item.storeQuantity || (item.quantity || "") + " " + (item.unit || "");
      lines.push("☐ " + item.name + " — " + qty.trim());
    });
    lines.push("");
  });
  return lines.join("\n");
}

function renderGroceryList(data) {
  state.lastGroceryData = data;
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
      '<p class="text-sm text-muted">' +
      emptyMessage +
      "</p>";
    if (data.allCoveredByPantry && data.pantrySubtractedCount) {
      emptyHtml +=
        '<p class="text-sm text-muted mt-2">' +
        esc(
          data.pantrySubtractedCount +
            " grocery items were adjusted using your pantry.",
        ) +
        "</p>";
    }
    container.innerHTML = emptyHtml;
    return;
  }

  var html = '<div style="animation: fadeIn 0.3s ease-out;">';
  html += '<div class="mb-6">';
  html += '<h2 style="font-size:1.5rem;font-weight:600;">Grocery List</h2>';
  html +=
    '<p class="text-sm text-muted mt-1">' +
    items.length +
    (items.length === 1 ? " item left to buy" : " items left to buy") +
    " &middot; " +
    coveredItems.length +
    (coveredItems.length === 1
      ? " item already in pantry"
      : " items already in pantry") +
    "</p>";
  if (data.pantrySubtractedCount) {
    html +=
      '<p class="text-sm text-muted mt-1">' +
      esc(
        data.pantrySubtractedCount +
          " grocery items adjusted using your pantry amounts",
      ) +
      "</p>";
  }
  if (!items.length && coveredItems.length) {
    html +=
      '<p class="text-sm text-muted mt-1">Everything you need for this plan is already covered by pantry items.</p>';
  }
  html += "</div>";

  if (items.length) {
    html += '<div style="display:flex;gap:0.5rem;margin-bottom:1rem;">';
    html += '<button class="btn-action" id="btn-copy-grocery">Copy Shopping List</button>';
    html += '<button class="btn-action" id="btn-print-grocery">Print Shopping List</button>';
    html += '</div>';
  }

  html += renderGrocerySection("Need to Buy", items, "remaining");
  html += renderGrocerySection("Already in Pantry", coveredItems, "covered");
  html += "</div>";

  container.innerHTML = html;
  bindGroceryControls(container);
}

/* =================================================================
   Preferences
   ================================================================= */
function loadPreferences() {
  Api.fetchPreferences()
    .then(function (prefs) {
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
    })
    .catch(function () {
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
    html +=
      '<button class="serving-btn' +
      (n === state.servingSize ? " active" : "") +
      '" data-n="' +
      n +
      '">' +
      n +
      "</button>";
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
    html +=
      '<button class="chip' +
      (active ? " active" : "") +
      '" data-diet="' +
      esc(diet) +
      '">' +
      esc(diet) +
      "</button>";
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
    html +=
      '<button class="chip' +
      (active ? " active" : "") +
      '" data-cuisine="' +
      esc(cuisine) +
      '">' +
      esc(cuisine) +
      "</button>";
  });
  container.innerHTML = html;
  container.querySelectorAll(".chip").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var cuisine = btn.getAttribute("data-cuisine");
      state.selectedCuisines[cuisine] = !state.selectedCuisines[cuisine];
      if (!state.selectedCuisines[cuisine])
        delete state.selectedCuisines[cuisine];
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
    html += '<span class="staple-category-title">' + esc(cat.name) + "</span>";
    html +=
      '<button class="staple-toggle-all" data-cat="' +
      esc(cat.name) +
      '">Toggle all</button>';
    html += "</div>";
    html += '<div class="staple-grid">';
    cat.items.forEach(function (item) {
      var on = !!state.selectedProteins[item];
      html +=
        '<button class="staple-item' +
        (on ? " on" : "") +
        '" data-item="' +
        esc(item) +
        '">';
      html += '<span class="staple-check">' + CHECK_SVG + "</span>";
      html += "<span>" + esc(item) + "</span>";
      html += "</button>";
    });
    html += "</div></div>";
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
      var cat = PROTEIN_OPTIONS.find(function (c) {
        return c.name === catName;
      });
      if (!cat) return;
      var allOn = cat.items.every(function (i) {
        return state.selectedProteins[i];
      });
      cat.items.forEach(function (i) {
        if (allOn) {
          delete state.selectedProteins[i];
        } else {
          state.selectedProteins[i] = true;
        }
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
    html += '<span class="staple-category-title">' + esc(cat.name) + "</span>";
    html +=
      '<button class="staple-toggle-all" data-cat="' +
      esc(cat.name) +
      '">Toggle all</button>';
    html += "</div>";
    html += '<div class="staple-grid">';
    cat.items.forEach(function (item) {
      var on = !!state.selectedVegetables[item];
      html +=
        '<button class="staple-item' +
        (on ? " on" : "") +
        '" data-item="' +
        esc(item) +
        '">';
      html += '<span class="staple-check">' + CHECK_SVG + "</span>";
      html += "<span>" + esc(item) + "</span>";
      html += "</button>";
    });
    html += "</div></div>";
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
      var cat = VEGETABLE_OPTIONS.find(function (c) {
        return c.name === catName;
      });
      if (!cat) return;
      var allOn = cat.items.every(function (i) {
        return state.selectedVegetables[i];
      });
      cat.items.forEach(function (i) {
        if (allOn) {
          delete state.selectedVegetables[i];
        } else {
          state.selectedVegetables[i] = true;
        }
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
  html +=
    '<button class="staple-toggle-all" id="fruit-toggle-all">Toggle all</button>';
  html += "</div>";
  html += '<div class="staple-grid">';
  FRUIT_OPTIONS.forEach(function (item) {
    var on = !!state.selectedFruits[item];
    html +=
      '<button class="staple-item' +
      (on ? " on" : "") +
      '" data-item="' +
      esc(item) +
      '">';
    html += '<span class="staple-check">' + CHECK_SVG + "</span>";
    html += "<span>" + esc(item) + "</span>";
    html += "</button>";
  });
  html += "</div></div>";

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
      var allOn = FRUIT_OPTIONS.every(function (f) {
        return state.selectedFruits[f];
      });
      FRUIT_OPTIONS.forEach(function (f) {
        if (allOn) {
          delete state.selectedFruits[f];
        } else {
          state.selectedFruits[f] = true;
        }
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
    preferredCuisines: cuisineStr || "",
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

  Api.generateMealPlan(payload)
    .then(function (plan) {
      state.mealPlan = plan;
      state.selectedMeal = null;
      state.checkedItems = {};
      state.viewingHistoricPlan = false;
      state.planHistoryIndex = 0;
      updatePlanSubtitle();
      renderMealGrid(plan.meals || []);
      updateHistoricPlanUI();
      loadPlanHistory();
      $("#recipe-panel").innerHTML =
        '<div class="empty-state"><div>' +
        "<p>Select a meal from the plan</p>" +
        "<p>Recipe details will appear here</p>" +
        "</div></div>";
    })
    .catch(function (err) {
      console.error("Generation failed:", err);
      var panel = $("#recipe-panel");
      panel.innerHTML =
        '<div class="empty-state"><div>' +
        '<p style="color:var(--destructive,#c44);">Failed to generate meal plan</p>' +
        "<p>We couldn't build a complete plan for the current preferences. Please try again in a moment.</p>" +
        "</div></div>";
    })
    .finally(function () {
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

  var exportBtn = document.getElementById("btn-export-week");
  if (exportBtn) {
    exportBtn.addEventListener("click", handleExportWeek);
  }

  fetch("/api/auth/me", { credentials: "same-origin" })
    .then(function (res) {
      return res.ok ? res.json() : { loggedIn: false };
    })
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

      if (Onboarding.resumeIfPending()) {
        revealApp();
        return;
      }

      Onboarding.check()
        .then(function (shown) {
          if (shown) {
            revealApp();
          } else {
            initApp();
          }
        })
        .catch(function () {
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
  Api.fetchMealPlan()
    .then(function (plan) {
      state.mealPlan = plan;
      updatePlanSubtitle();
      renderMealGrid(plan.meals || []);
    })
    .catch(function () {
      renderMealGrid([]);
    });

  loadPlanHistory();
  renderPreferences();
}
