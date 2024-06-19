package com.example.lottooptionspro;

import reactor.core.publisher.Mono;

public interface GameInformation {
     Mono<Void> setUpUi(String stateName, String gameName);
}
