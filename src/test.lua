-- Un fichier Lua avec des fonctions interconnectées

-- Fonction principale
function main()
    print("Démarrage du programme")
    local result = calculate(5, 10)
    print("Résultat du calcul :", result)

    local isValid = validate(result)
    if isValid then
        print("Le résultat est valide. Déclenchement de l'action...")
        triggerAction(result)
    else
        print("Le résultat n'est pas valide. Annulation.")
    end

    print("Fin du programme.")
end

-- Fonction pour effectuer un calcul
function calculate(a, b)
    local sum = add(a, b)
    local product = multiply(a, b)
    return combine(sum, product)
end

-- Fonction pour additionner deux nombres
function add(x, y)
    print("Addition de", x, "et", y)
    return x + y
end

-- Fonction pour multiplier deux nombres
function multiply(x, y)
    print("Multiplication de", x, "et", y)
    return x * y
end

-- Fonction pour combiner les résultats
function combine(sum, product)
    print("Combinaison des résultats :", sum, "et", product)
    return sum + product
end

-- Fonction pour valider un résultat
function validate(value)
    print("Validation de la valeur :", value)
    return value % 2 == 0 -- Retourne vrai si la valeur est paire
end

-- Fonction pour déclencher une action
function triggerAction(value)
    print("Déclenchement de l'action avec la valeur :", value)
    if value > 50 then
        highValueAction(value)
    else
        lowValueAction(value)
    end
end

-- Fonction pour une action de haute valeur
function highValueAction(value)
    print("Action pour une haute valeur :", value)
    logAction("High value action triggered with " .. value)
end

-- Fonction pour une action de basse valeur
function lowValueAction(value)
    print("Action pour une basse valeur :", value)
    logAction("Low value action triggered with " .. value)
end

-- Fonction pour enregistrer une action dans un journal
function logAction(message)
    print("Enregistrement dans le journal :", message)
    -- Simule l'écriture dans un fichier (non implémentée ici)
end

-- Exécution du programme
main()
