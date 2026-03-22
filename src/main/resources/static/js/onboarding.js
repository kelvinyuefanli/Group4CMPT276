"use strict";

var Onboarding = (function () {

  var TOTAL_STEPS = 11;

  var DIET_OPTIONS = [
    "Vegetarian", "Vegan", "Gluten-Free", "Dairy-Free",
    "Nut-Free", "Low-Carb", "Halal", "Kosher"
  ];

  var ALLERGY_OPTIONS = [
    "Peanuts", "Tree Nuts", "Shellfish", "Fish",
    "Eggs", "Milk", "Soy", "Wheat", "Sesame"
  ];

  var CUISINE_OPTIONS = [
    "Italian", "Mexican", "Chinese", "Japanese", "Korean",
    "Thai", "Vietnamese", "Indian", "Mediterranean", "French",
    "American", "Middle Eastern", "Greek", "Caribbean", "Spanish"
  ];

  var DAYS = ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"];
  var DAY_SHORT = { MONDAY:"Mon", TUESDAY:"Tue", WEDNESDAY:"Wed", THURSDAY:"Thu", FRIDAY:"Fri", SATURDAY:"Sat", SUNDAY:"Sun" };
  var MEALS = ["BREAKFAST","LUNCH","DINNER"];

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

  var PANTRY_CATEGORIES = [
    { name: "Oils & Condiments", preChecked: true, items: [
      "Olive Oil","Vegetable Oil","Soy Sauce","Vinegar","Hot Sauce","Ketchup","Mustard","Mayonnaise"
    ]},
    { name: "Spices & Seasonings", preChecked: true, items: [
      "Salt","Black Pepper","Garlic Powder","Onion Powder","Cumin","Paprika","Oregano","Cinnamon","Chili Flakes","Italian Seasoning"
    ]},
    { name: "Baking & Dry Goods", preChecked: false, items: [
      "Flour","Sugar","Brown Sugar","Baking Powder","Baking Soda","Cornstarch","Vanilla Extract"
    ]},
    { name: "Canned & Jarred", preChecked: false, items: [
      "Canned Tomatoes","Tomato Paste","Chicken Broth","Coconut Milk","Beans (canned)"
    ]},
    { name: "Always on Hand", preChecked: false, items: [
      "Butter","Eggs","Milk","Garlic","Onions","Rice","Pasta","Bread"
    ]}
  ];

  /* ---- state ---- */
  var step = 1;
  var data = {
    servingSize: 2,
    diets: {},
    allergies: {},
    allergyOther: "",
    cuisines: {},
    rotateCuisines: true,
    dislikedFoods: [],
    schedule: {},
    pantry: {},
    proteins: {},
    vegetables: {},
    fruits: {}
  };

  function init() {
    DAYS.forEach(function (d) {
      data.schedule[d] = {};
      MEALS.forEach(function (m) { data.schedule[d][m] = true; });
    });
    PANTRY_CATEGORIES.forEach(function (cat) {
      cat.items.forEach(function (item) {
        data.pantry[item] = cat.preChecked;
      });
    });
  }

  /* ---- helpers ---- */
  function esc(s) {
    if (s == null) return "";
    var d = document.createElement("div");
    d.appendChild(document.createTextNode(String(s)));
    return d.innerHTML;
  }

  var CHECK_SVG = '<svg width="10" height="8" viewBox="0 0 10 8" fill="none"><path d="M1 4L3.5 6.5L9 1" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';

  /* ---- render framework ---- */
  function render() {
    var overlay = document.getElementById("onboarding-overlay");
    var html = '<div class="onboarding-card">';
    html += renderProgress();
    html += renderStepContent();
    html += '</div>';
    overlay.innerHTML = html;
    overlay.removeAttribute("hidden");
    bindStep();
  }

  function renderProgress() {
    if (step === 1) return "";
    var html = '<div class="onboarding-progress">';
    for (var i = 1; i <= TOTAL_STEPS; i++) {
      var cls = "progress-dot";
      if (i < step) cls += " done";
      if (i === step) cls += " active";
      html += '<span class="' + cls + '"></span>';
    }
    html += '</div>';
    return html;
  }

  function nav(backLabel, nextLabel, nextDisabled) {
    var html = '<div class="onboarding-nav">';
    if (backLabel) {
      html += '<button class="btn-back" id="ob-back">' + esc(backLabel) + '</button>';
    } else {
      html += '<span></span>';
    }
    if (nextLabel) {
      html += '<button class="btn-next" id="ob-next"' + (nextDisabled ? " disabled" : "") + '>' + esc(nextLabel) + '</button>';
    }
    html += '</div>';
    return html;
  }

  /* ---- step renderers ---- */
  function renderStepContent() {
    switch (step) {
      case 1: return stepWelcome();
      case 2: return stepHousehold();
      case 3: return stepDietary();
      case 4: return stepAllergies();
      case 5: return stepCuisines();
      case 6: return stepDislikes();
      case 7: return stepSchedule();
      case 8: return stepPantry();
      case 9: return stepProteins();
      case 10: return stepVeggiesFruits();
      case 11: return stepReview();
      default: return "";
    }
  }

  /* Step 1: Welcome */
  function stepWelcome() {
    var html = '<div style="text-align:center;padding:2rem 0;">';
    html += '<div class="logo" style="font-size:2rem;margin-bottom:1.5rem;">Smart<span class="logo-accent">Cart</span></div>';
    html += '<h1 class="onboarding-title">Welcome to SmartCart</h1>';
    html += '<p class="onboarding-subtitle">Let\'s set up your profile in about 2 minutes so we can create your perfect meal plan.</p>';
    html += '<button class="btn-next btn-next-full" id="ob-next">Get Started</button>';
    html += '</div>';
    return html;
  }

  /* Step 2: Household */
  function stepHousehold() {
    var html = '<h1 class="onboarding-title">How many people are you cooking for?</h1>';
    html += '<p class="onboarding-subtitle">We\'ll adjust portion sizes for your household.</p>';
    html += '<div class="serving-btns" style="justify-content:center;" id="ob-servings">';
    for (var n = 1; n <= 6; n++) {
      html += '<button class="serving-btn' + (n === data.servingSize ? " active" : "") + '" data-n="' + n + '">' + n + '</button>';
    }
    html += '</div>';
    html += nav("Back", "Next");
    return html;
  }

  /* Step 3: Dietary */
  function stepDietary() {
    var html = '<h1 class="onboarding-title">Do you follow any of these diets?</h1>';
    html += '<p class="onboarding-subtitle">Select all that apply, or skip if none.</p>';
    html += '<div class="chip-group" style="justify-content:center;" id="ob-diets">';
    var hasAnyDiet = Object.keys(data.diets).some(function (k) { return data.diets[k]; });
    html += '<button class="chip' + (!hasAnyDiet ? " active" : "") + '" data-val="__none__">None</button>';
    DIET_OPTIONS.forEach(function (d) {
      html += '<button class="chip' + (data.diets[d] ? " active" : "") + '" data-val="' + esc(d) + '">' + esc(d) + '</button>';
    });
    html += '</div>';
    html += nav("Back", "Next");
    return html;
  }

  /* Step 4: Allergies */
  function stepAllergies() {
    var html = '<h1 class="onboarding-title">Any food allergies?</h1>';
    html += '<p class="onboarding-subtitle">We\'ll make sure these never appear in your recipes.</p>';
    html += '<div class="chip-group" style="justify-content:center;" id="ob-allergies">';
    var hasAnyAllergy = Object.keys(data.allergies).some(function (k) { return data.allergies[k]; });
    html += '<button class="chip' + (!hasAnyAllergy && !data.allergyOther ? " active" : "") + '" data-val="__none__">None</button>';
    ALLERGY_OPTIONS.forEach(function (a) {
      html += '<button class="chip' + (data.allergies[a] ? " active" : "") + '" data-val="' + esc(a) + '">' + esc(a) + '</button>';
    });
    html += '</div>';
    html += '<div style="margin-top:1rem;">';
    html += '<input class="text-input" id="ob-allergy-other" placeholder="e.g. mustard, celery, lupin..." value="' + esc(data.allergyOther) + '" />';
    html += '</div>';
    html += nav("Back", "Next");
    return html;
  }

  /* Step 5: Cuisines */
  function stepCuisines() {
    var html = '<h1 class="onboarding-title">What cuisines do you enjoy?</h1>';
    html += '<p class="onboarding-subtitle">Select a few or type your own below. Press Enter to add.</p>';
    html += '<div class="chip-group" style="justify-content:center;" id="ob-cuisines">';
    CUISINE_OPTIONS.forEach(function (c) {
      html += '<button class="chip' + (data.cuisines[c] ? " active" : "") + '" data-val="' + esc(c) + '">' + esc(c) + '</button>';
    });
    var customCuisines = Object.keys(data.cuisines).filter(function (c) {
      return data.cuisines[c] && CUISINE_OPTIONS.indexOf(c) === -1;
    });
    customCuisines.forEach(function (c) {
      html += '<button class="chip active" data-val="' + esc(c) + '">' + esc(c) + '</button>';
    });
    html += '</div>';
    html += '<div style="margin-top:0.75rem;">';
    html += '<input class="text-input" id="ob-cuisine-input" placeholder="e.g. Brazilian, Peruvian, Filipino, BBQ..." />';
    html += '</div>';
    html += '<div class="toggle-row" style="justify-content:center;" id="ob-rotate">';
    html += '<button class="toggle-track' + (data.rotateCuisines ? " on" : "") + '" id="ob-rotate-btn"><span class="toggle-thumb"></span></button>';
    html += '<span>Rotate cuisines across the week</span>';
    html += '</div>';
    html += nav("Back", "Next");
    return html;
  }

  /* Step 6: Dislikes */
  function stepDislikes() {
    var html = '<h1 class="onboarding-title">Any foods you\'d rather avoid?</h1>';
    html += '<p class="onboarding-subtitle">Type foods separated by commas and press Enter. We\'ll keep these out of your recipes.</p>';

    html += '<input class="text-input" id="ob-dislike-input" placeholder="e.g. cilantro, liver, mushrooms, olives..." />';
    html += '<div class="tag-group" id="ob-dislike-tags">';
    data.dislikedFoods.forEach(function (f) {
      html += '<span class="tag">' + esc(f) + '<button class="tag-remove" data-val="' + esc(f) + '">&times;</button></span>';
    });
    html += '</div>';

    html += nav("Back", "Next");
    return html;
  }

  /* Step 7: Schedule */
  function stepSchedule() {
    var html = '<h1 class="onboarding-title">Which meals do you want planned?</h1>';
    html += '<p class="onboarding-subtitle">Uncheck any meals you\'d rather skip.</p>';

    html += '<div class="schedule-quick-btns" id="ob-quick">';
    ["Every meal","Weekdays only","All Breakfasts","All Lunches","All Dinners"].forEach(function (label) {
      html += '<button class="quick-btn" data-action="' + esc(label) + '">' + esc(label) + '</button>';
    });
    html += '</div>';

    html += '<div class="schedule-grid" id="ob-schedule">';
    html += '<span class="sg-header"></span>';
    MEALS.forEach(function (m) {
      html += '<span class="sg-header">' + m.charAt(0) + m.slice(1).toLowerCase() + '</span>';
    });
    DAYS.forEach(function (d) {
      html += '<span class="sg-day">' + DAY_SHORT[d] + '</span>';
      MEALS.forEach(function (m) {
        var on = data.schedule[d][m];
        html += '<span class="sg-cell"><button class="sg-check' + (on ? " on" : "") + '" data-day="' + d + '" data-meal="' + m + '">' + CHECK_SVG + '</button></span>';
      });
    });
    html += '</div>';

    html += nav("Back", "Next");
    return html;
  }

  /* Step 8: Pantry */
  function stepPantry() {
    var html = '<h1 class="onboarding-title">What\'s in your kitchen?</h1>';
    html += '<p class="onboarding-subtitle">We\'ll skip these when building your grocery list.</p>';

    PANTRY_CATEGORIES.forEach(function (cat) {
      html += '<div class="staple-category">';
      html += '<div class="staple-category-header">';
      html += '<span class="staple-category-title">' + esc(cat.name) + '</span>';
      html += '<button class="staple-toggle-all" data-cat="' + esc(cat.name) + '">Toggle all</button>';
      html += '</div>';
      html += '<div class="staple-grid">';
      cat.items.forEach(function (item) {
        var on = data.pantry[item];
        html += '<button class="staple-item' + (on ? " on" : "") + '" data-item="' + esc(item) + '">';
        html += '<span class="staple-check">' + CHECK_SVG + '</span>';
        html += '<span>' + esc(item) + '</span>';
        html += '</button>';
      });
      html += '</div></div>';
    });

    html += '<div style="margin-top:0.75rem;">';
    html += '<input class="text-input" id="ob-pantry-other" placeholder="e.g. soy sauce, flour, sriracha, rice vinegar..." />';
    html += '</div>';
    html += nav("Back", "Next");
    return html;
  }

  /* Step 9: Weekly Proteins */
  function stepProteins() {
    var html = '<h1 class="onboarding-title">What proteins do you want this week?</h1>';
    html += '<p class="onboarding-subtitle">Pick 2-3 proteins to buy. We\'ll plan portions across your meals so nothing goes to waste.</p>';

    PROTEIN_OPTIONS.forEach(function (cat) {
      html += '<div class="staple-category">';
      html += '<div class="staple-category-header">';
      html += '<span class="staple-category-title">' + esc(cat.name) + '</span>';
      html += '</div>';
      html += '<div class="staple-grid">';
      cat.items.forEach(function (item) {
        var on = data.proteins[item];
        html += '<button class="staple-item' + (on ? " on" : "") + '" data-item="' + esc(item) + '">';
        html += '<span class="staple-check">' + CHECK_SVG + '</span>';
        html += '<span>' + esc(item) + '</span>';
        html += '</button>';
      });
      html += '</div></div>';
    });

    var count = Object.keys(data.proteins).filter(function (k) { return data.proteins[k]; }).length;
    html += '<p class="hint" style="text-align:center;margin-top:0.5rem;">' + count + ' selected — we recommend 2-3 proteins per week</p>';
    html += nav("Back", "Next");
    return html;
  }

  /* Step 10: Weekly Vegetables & Fruits */
  function stepVeggiesFruits() {
    var html = '<h1 class="onboarding-title">Which vegetables and fruits to buy?</h1>';
    html += '<p class="onboarding-subtitle">Pick 4-6 vegetables and 2-3 fruits. We\'ll plan recipes so each item is used across 3-4 meals.</p>';

    html += '<h2 style="font-size:1rem;margin:1rem 0 0.5rem;color:var(--foreground);">Vegetables</h2>';
    html += '<div id="ob-veggies">';
    VEGETABLE_OPTIONS.forEach(function (cat) {
      html += '<div class="staple-category">';
      html += '<div class="staple-category-header">';
      html += '<span class="staple-category-title">' + esc(cat.name) + '</span>';
      html += '</div>';
      html += '<div class="staple-grid">';
      cat.items.forEach(function (item) {
        var on = data.vegetables[item];
        html += '<button class="staple-item' + (on ? " on" : "") + '" data-item="' + esc(item) + '">';
        html += '<span class="staple-check">' + CHECK_SVG + '</span>';
        html += '<span>' + esc(item) + '</span>';
        html += '</button>';
      });
      html += '</div></div>';
    });
    html += '</div>';

    html += '<h2 style="font-size:1rem;margin:1rem 0 0.5rem;color:var(--foreground);">Fruits</h2>';
    html += '<div class="staple-grid" id="ob-fruits">';
    FRUIT_OPTIONS.forEach(function (item) {
      var on = data.fruits[item];
      html += '<button class="staple-item' + (on ? " on" : "") + '" data-item="' + esc(item) + '">';
      html += '<span class="staple-check">' + CHECK_SVG + '</span>';
      html += '<span>' + esc(item) + '</span>';
      html += '</button>';
    });
    html += '</div>';

    var vegCount = Object.keys(data.vegetables).filter(function (k) { return data.vegetables[k]; }).length;
    var fruitCount = Object.keys(data.fruits).filter(function (k) { return data.fruits[k]; }).length;
    html += '<p class="hint" style="text-align:center;margin-top:0.5rem;">' + vegCount + ' vegetables, ' + fruitCount + ' fruits selected</p>';
    html += nav("Back", "Next");
    return html;
  }

  /* Step 11: Review */
  function stepReview() {
    var html = '<h1 class="onboarding-title">Here\'s your profile</h1>';
    html += '<p class="onboarding-subtitle">Review and create your first meal plan.</p>';

    html += reviewSection("Household", data.servingSize + (data.servingSize === 1 ? " person" : " people"), 2);

    var dietList = Object.keys(data.diets).filter(function (k) { return data.diets[k]; });
    html += reviewSection("Dietary Restrictions", dietList.length ? dietList.join(", ") : "None", 3);

    var allergyList = Object.keys(data.allergies).filter(function (k) { return data.allergies[k]; });
    if (data.allergyOther) allergyList.push(data.allergyOther);
    html += reviewSection("Allergies", allergyList.length ? allergyList.join(", ") : "None", 4);

    var cuisineList = Object.keys(data.cuisines).filter(function (k) { return data.cuisines[k]; });
    var cuisineText = cuisineList.length ? cuisineList.join(", ") : "Any";
    if (data.rotateCuisines && cuisineList.length > 1) cuisineText += " (rotating)";
    html += reviewSection("Cuisines", cuisineText, 5);

    html += reviewSection("Dislikes", data.dislikedFoods.length ? data.dislikedFoods.join(", ") : "None", 6);

    var scheduleCount = 0;
    DAYS.forEach(function (d) { MEALS.forEach(function (m) { if (data.schedule[d][m]) scheduleCount++; }); });
    html += reviewSection("Meal Schedule", scheduleCount + " of 21 meals", 7);

    var pantryItems = Object.keys(data.pantry).filter(function (k) { return data.pantry[k]; });
    var pantryText = pantryItems.length ? pantryItems.length + " items" : "None";
    html += reviewSection("Pantry Staples", pantryText, 8);

    var proteinList = Object.keys(data.proteins).filter(function (k) { return data.proteins[k]; });
    html += reviewSection("Weekly Proteins", proteinList.length ? proteinList.join(", ") : "Any", 9);

    var vegList = Object.keys(data.vegetables).filter(function (k) { return data.vegetables[k]; });
    var fruitList = Object.keys(data.fruits).filter(function (k) { return data.fruits[k]; });
    var produceText = "";
    if (vegList.length) produceText += vegList.length + " vegetables";
    if (fruitList.length) produceText += (produceText ? ", " : "") + fruitList.length + " fruits";
    if (!produceText) produceText = "Any";
    html += reviewSection("Weekly Produce", produceText, 10);

    html += '<div style="margin-top:1.5rem;">';
    html += '<button class="btn-next btn-next-full" id="ob-next">Create My First Meal Plan</button>';
    html += '<p class="hint" style="text-align:center;">This will generate a personalized 7-day plan</p>';
    html += '</div>';

    html += '<div style="text-align:center;margin-top:0.75rem;">';
    html += '<button class="btn-back" id="ob-back">Back</button>';
    html += '</div>';
    return html;
  }

  function reviewSection(title, value, goStep) {
    var html = '<div class="review-section">';
    html += '<div class="review-section-header">';
    html += '<span class="review-section-title">' + esc(title) + '</span>';
    html += '<button class="review-edit" data-go="' + goStep + '">Edit</button>';
    html += '</div>';
    html += '<div class="review-value">' + esc(value) + '</div>';
    html += '</div>';
    return html;
  }

  /* ---- event binding ---- */
  function bindStep() {
    var back = document.getElementById("ob-back");
    var next = document.getElementById("ob-next");
    if (back) back.addEventListener("click", function () { step--; render(); });
    if (next) next.addEventListener("click", function () {
      captureInput();
      if (step < TOTAL_STEPS) { step++; render(); }
      else { submitOnboarding(); }
    });

    /* Step-specific bindings */
    switch (step) {
      case 2: bindServings(); break;
      case 3: bindChipGroup("ob-diets", data.diets); break;
      case 4: bindChipGroup("ob-allergies", data.allergies); break;
      case 5: bindChipGroup("ob-cuisines", data.cuisines); bindCuisineInput(); bindRotate(); break;
      case 6: bindDislikes(); break;
      case 7: bindSchedule(); break;
      case 8: bindPantry(); break;
      case 9: bindItemPickerAll(data.proteins); break;
      case 10: bindItemPickerIn("ob-veggies", data.vegetables); bindItemPickerIn("ob-fruits", data.fruits); break;
      case 11: bindReviewEdits(); break;
    }
  }

  function captureInput() {
    if (step === 4) {
      var input = document.getElementById("ob-allergy-other");
      if (input) data.allergyOther = input.value.trim();
    }
  }

  function bindServings() {
    document.querySelectorAll("#ob-servings .serving-btn").forEach(function (btn) {
      btn.addEventListener("click", function () {
        data.servingSize = Number(btn.getAttribute("data-n"));
        document.querySelectorAll("#ob-servings .serving-btn").forEach(function (b) {
          b.classList.toggle("active", Number(b.getAttribute("data-n")) === data.servingSize);
        });
      });
    });
  }

  function bindChipGroup(containerId, store) {
    var container = document.getElementById(containerId);
    if (!container) return;
    container.querySelectorAll(".chip").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var val = btn.getAttribute("data-val");
        if (val === "__none__") {
          Object.keys(store).forEach(function (k) { delete store[k]; });
          container.querySelectorAll(".chip").forEach(function (b) {
            b.classList.toggle("active", b.getAttribute("data-val") === "__none__");
          });
        } else {
          store[val] = !store[val];
          if (!store[val]) delete store[val];
          btn.classList.toggle("active", !!store[val]);
          var noneBtn = container.querySelector('.chip[data-val="__none__"]');
          if (noneBtn) {
            var hasAny = Object.keys(store).some(function (k) { return store[k]; });
            noneBtn.classList.toggle("active", !hasAny);
          }
        }
      });
    });
  }

  function bindCuisineInput() {
    var input = document.getElementById("ob-cuisine-input");
    if (input) {
      input.addEventListener("keydown", function (e) {
        if (e.key === "Enter" && input.value.trim()) {
          input.value.split(",").forEach(function (part) {
            var v = part.trim();
            if (v) data.cuisines[v] = true;
          });
          input.value = "";
          render();
        }
      });
    }
  }

  function bindRotate() {
    var btn = document.getElementById("ob-rotate-btn");
    if (btn) btn.addEventListener("click", function () {
      data.rotateCuisines = !data.rotateCuisines;
      btn.classList.toggle("on", data.rotateCuisines);
    });
  }

  function bindDislikes() {
    var input = document.getElementById("ob-dislike-input");
    if (input) {
      input.addEventListener("keydown", function (e) {
        if (e.key === "Enter" && input.value.trim()) {
          input.value.split(",").forEach(function (part) {
            var v = part.trim();
            if (v && data.dislikedFoods.indexOf(v) === -1) {
              data.dislikedFoods.push(v);
            }
          });
          input.value = "";
          render();
        }
      });
    }
    document.querySelectorAll("#ob-dislike-tags .tag-remove").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var val = btn.getAttribute("data-val");
        data.dislikedFoods = data.dislikedFoods.filter(function (f) { return f !== val; });
        render();
      });
    });
  }

  function bindSchedule() {
    document.querySelectorAll("#ob-schedule .sg-check").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var d = btn.getAttribute("data-day");
        var m = btn.getAttribute("data-meal");
        data.schedule[d][m] = !data.schedule[d][m];
        btn.classList.toggle("on", data.schedule[d][m]);
      });
    });
    document.querySelectorAll("#ob-quick .quick-btn").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var action = btn.getAttribute("data-action");
        if (action === "Every meal") {
          DAYS.forEach(function (d) { MEALS.forEach(function (m) { data.schedule[d][m] = true; }); });
        } else if (action === "Weekdays only") {
          DAYS.forEach(function (d) {
            var wd = d !== "SATURDAY" && d !== "SUNDAY";
            MEALS.forEach(function (m) { data.schedule[d][m] = wd; });
          });
        } else if (action === "All Breakfasts") {
          var allOn = DAYS.every(function (d) { return data.schedule[d].BREAKFAST; });
          DAYS.forEach(function (d) { data.schedule[d].BREAKFAST = !allOn; });
        } else if (action === "All Lunches") {
          var allOnL = DAYS.every(function (d) { return data.schedule[d].LUNCH; });
          DAYS.forEach(function (d) { data.schedule[d].LUNCH = !allOnL; });
        } else if (action === "All Dinners") {
          var allOnD = DAYS.every(function (d) { return data.schedule[d].DINNER; });
          DAYS.forEach(function (d) { data.schedule[d].DINNER = !allOnD; });
        }
        render();
      });
    });
  }

  function bindPantry() {
    document.querySelectorAll(".staple-item").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var item = btn.getAttribute("data-item");
        data.pantry[item] = !data.pantry[item];
        btn.classList.toggle("on", data.pantry[item]);
      });
    });
    document.querySelectorAll(".staple-toggle-all").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var catName = btn.getAttribute("data-cat");
        var cat = PANTRY_CATEGORIES.find(function (c) { return c.name === catName; });
        if (!cat) return;
        var allOn = cat.items.every(function (i) { return data.pantry[i]; });
        cat.items.forEach(function (i) { data.pantry[i] = !allOn; });
        render();
      });
    });
    var otherInput = document.getElementById("ob-pantry-other");
    if (otherInput) {
      otherInput.addEventListener("keydown", function (e) {
        if (e.key === "Enter" && otherInput.value.trim()) {
          otherInput.value.split(",").forEach(function (part) {
            var v = part.trim();
            if (v) data.pantry[v] = true;
          });
          otherInput.value = "";
          render();
        }
      });
    }
  }

  function bindItemPickerAll(store) {
    document.querySelectorAll(".staple-item").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var item = btn.getAttribute("data-item");
        store[item] = !store[item];
        if (!store[item]) delete store[item];
        btn.classList.toggle("on", !!store[item]);
      });
    });
  }

  function bindItemPickerIn(containerId, store) {
    var container = document.getElementById(containerId);
    if (!container) return;
    container.querySelectorAll(".staple-item").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var item = btn.getAttribute("data-item");
        store[item] = !store[item];
        if (!store[item]) delete store[item];
        btn.classList.toggle("on", !!store[item]);
      });
    });
  }

  function bindReviewEdits() {
    document.querySelectorAll(".review-edit").forEach(function (btn) {
      btn.addEventListener("click", function () {
        step = Number(btn.getAttribute("data-go"));
        render();
      });
    });
  }

  /* ---- build payload from in-memory data ---- */
  function buildPayload() {
    var dietList = Object.keys(data.diets).filter(function (k) { return data.diets[k]; });
    var allergyList = Object.keys(data.allergies).filter(function (k) { return data.allergies[k]; });
    if (data.allergyOther) allergyList.push(data.allergyOther);
    var cuisineList = Object.keys(data.cuisines).filter(function (k) { return data.cuisines[k]; });
    var pantryItems = Object.keys(data.pantry).filter(function (k) { return data.pantry[k]; });

    var scheduleObj = {};
    DAYS.forEach(function (d) {
      var meals = MEALS.filter(function (m) { return data.schedule[d][m]; });
      if (meals.length) scheduleObj[d] = meals;
    });

    var proteinList = Object.keys(data.proteins).filter(function (k) { return data.proteins[k]; });
    var vegList = Object.keys(data.vegetables).filter(function (k) { return data.vegetables[k]; });
    var fruitList = Object.keys(data.fruits).filter(function (k) { return data.fruits[k]; });

    return {
      prefs: {
        servingSize: data.servingSize,
        dietaryRestrictions: dietList.join(", ") || null,
        allergies: allergyList.join(", ") || null,
        preferredCuisines: cuisineList.join(", ") || null,
        rotateCuisines: data.rotateCuisines,
        dislikedFoods: data.dislikedFoods.join(", ") || null,
        mealSchedule: JSON.stringify(scheduleObj),
        preferredProteins: proteinList.join(", ") || null,
        preferredVegetables: vegList.join(", ") || null,
        preferredFruits: fruitList.join(", ") || null,
        onboardingCompleted: true
      },
      pantryItems: pantryItems
    };
  }

  /* ---- persist onboarding to server (user must be authenticated) ---- */
  function persistOnboarding(payload) {
    var overlay = document.getElementById("onboarding-overlay");
    var card = overlay.querySelector(".onboarding-card");
    card.innerHTML =
      '<div style="text-align:center;padding:3rem 0;">' +
      '<div class="logo" style="font-size:1.5rem;margin-bottom:1rem;">Smart<span class="logo-accent">Cart</span></div>' +
      '<p class="onboarding-subtitle">Creating your personalized meal plan...</p>' +
      '<p class="text-muted text-sm">This may take a minute</p>' +
      '</div>';

    Api.updatePreferences(payload.prefs)
      .then(function () { return Api.savePantry(payload.pantryItems); })
      .then(function () { return Api.generateMealPlan(payload.pantryItems.join(", ")); })
      .then(function (plan) {
        sessionStorage.removeItem("onboardingPayload");
        overlay.setAttribute("hidden", "");
        document.querySelector(".app").style.display = "";
        if (typeof state !== "undefined") {
          state.mealPlan = plan;
          state.selectedMeal = null;
          state.checkedItems = {};
          state.servingSize = payload.prefs.servingSize;
          updatePlanSubtitle();
          renderMealGrid(plan.meals || []);
          renderPreferences();
        }
      })
      .catch(function (err) {
        console.error("Onboarding submit failed:", err);
        card.innerHTML =
          '<div style="text-align:center;padding:2rem 0;">' +
          '<h2 class="onboarding-title">Something went wrong</h2>' +
          '<p class="onboarding-subtitle">We saved your preferences but couldn\'t generate a plan. You can try again from the Preferences page.</p>' +
          '<button class="btn-next" id="ob-dismiss">Go to App</button>' +
          '</div>';
        document.getElementById("ob-dismiss").addEventListener("click", function () {
          sessionStorage.removeItem("onboardingPayload");
          overlay.setAttribute("hidden", "");
          document.querySelector(".app").style.display = "";
          if (typeof loadPreferences === "function") loadPreferences();
          if (typeof renderMealGrid === "function") renderMealGrid([]);
        });
      });
  }

  /* ---- submit: stash data then require signup ---- */
  function submitOnboarding() {
    var payload = buildPayload();

    fetch("/api/auth/me", { credentials: "same-origin" })
      .then(function (res) { return res.ok ? res.json() : { loggedIn: false }; })
      .then(function (auth) {
        if (auth.loggedIn) {
          persistOnboarding(payload);
        } else {
          sessionStorage.setItem("onboardingPayload", JSON.stringify(payload));
          window.location.href = "/register.html?from=onboarding";
        }
      })
      .catch(function () {
        sessionStorage.setItem("onboardingPayload", JSON.stringify(payload));
        window.location.href = "/register.html?from=onboarding";
      });
  }

  /* ---- public ---- */
  return {
    check: function () {
      return Api.fetchPreferences().then(function (prefs) {
        if (prefs && prefs.onboardingCompleted) return false;
        init();
        render();
        document.querySelector(".app").style.display = "none";
        return true;
      }).catch(function () { return false; });
    },
    resumeIfPending: function () {
      var raw = sessionStorage.getItem("onboardingPayload");
      if (!raw) return false;
      try {
        var payload = JSON.parse(raw);
        var overlay = document.getElementById("onboarding-overlay");
        overlay.removeAttribute("hidden");
        document.querySelector(".app").style.display = "none";
        overlay.innerHTML = '<div class="onboarding-card"></div>';
        persistOnboarding(payload);
        return true;
      } catch (e) {
        sessionStorage.removeItem("onboardingPayload");
        return false;
      }
    }
  };
})();
