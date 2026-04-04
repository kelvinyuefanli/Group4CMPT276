"use strict";

var Api = (function () {
  var BASE = "/api";

  /** Reads the XSRF-TOKEN cookie set by Spring Security's CookieCsrfTokenRepository. */
  function getCsrfToken() {
    var match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
    return match ? decodeURIComponent(match[1]) : null;
  }

  function request(path, opts) {
    var url = BASE + path;
    var headers = { "Content-Type": "application/json" };
    var token = getCsrfToken();
    if (token) {
      headers["X-XSRF-TOKEN"] = token;
    }
    var options = Object.assign({ headers: headers }, opts || {});
    return fetch(url, options).then(function (res) {
      if (!res.ok) {
        return res.text().then(function (body) {
          throw new Error("API " + res.status + ": " + body);
        });
      }
      return res.json();
    });
  }

  return {
    fetchMealPlan: function () {
      return request("/meal-plan");
    },

    generateMealPlan: function (data) {
      var payload;
      if (typeof data === "string" || data == null) {
        payload = { pantryIngredients: data || "" };
      } else {
        payload = Object.assign({ pantryIngredients: "" }, data);
      }
      return request("/meal-plan/generate", {
        method: "POST",
        body: JSON.stringify(payload),
      });
    },

    fetchRecipe: function (id) {
      return request("/recipes/" + id);
    },

    fetchGroceryList: function () {
      return request("/grocery-list");
    },

    createInstacartShoppingList: function () {
      return request("/instacart/shopping-list", {
        method: "POST",
        body: JSON.stringify({}),
      });
    },

    fetchPreferences: function () {
      return request("/preferences");
    },

    updatePreferences: function (data) {
      return request("/preferences", {
        method: "PUT",
        body: JSON.stringify(data),
      });
    },

    getPantry: function () {
      return request("/pantry");
    },

    savePantry: function (items) {
      return request("/pantry", {
        method: "POST",
        body: JSON.stringify({ items: items }),
      });
    },

    updatePantryItem: function (item) {
      return request("/pantry/item", {
        method: "PUT",
        body: JSON.stringify(item),
      });
    },

    deletePantryItem: function (id) {
      return request("/pantry/" + id, { method: "DELETE" });
    },

    swapMeals: function (slots) {
      return request("/meal-plan/swap", {
        method: "POST",
        body: JSON.stringify({ slots: slots }),
      });
    },
  };
})();
