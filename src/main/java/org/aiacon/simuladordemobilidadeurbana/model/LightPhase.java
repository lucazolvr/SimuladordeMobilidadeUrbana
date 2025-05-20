package org.aiacon.simuladordemobilidadeurbana.model; // Ou onde fizer sentido

public enum LightPhase {
    // Fases para um cruzamento padrão de 4 braços, onde um par de vias opostas fica verde/amarelo
    // enquanto o outro par fica vermelho.
    NS_GREEN_EW_RED,  // Norte-Sul Verde, Leste-Oeste Vermelho
    NS_YELLOW_EW_RED, // Norte-Sul Amarelo, Leste-Oeste Vermelho
    NS_RED_EW_GREEN,  // Leste-Oeste Verde, Norte-Sul Vermelho
    NS_RED_EW_YELLOW

    // Você poderia adicionar mais fases complexas se necessário (ex: setas dedicadas)
}