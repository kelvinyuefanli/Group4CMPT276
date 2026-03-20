"use strict";

var Api = (function () {
  var BASE = "/api";

  function request(path, opts) {
    var url = BASE + path;
    var options = Object.assign(
      { headers: { "Content-Type": "application/json" } },
      opts || {}
    );
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
  };
})();
