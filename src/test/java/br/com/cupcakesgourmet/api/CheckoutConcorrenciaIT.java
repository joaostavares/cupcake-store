package br.com.cupcakesgourmet.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Concorrência no H2 (sempre roda). */
@SpringBootTest
@ActiveProfiles("test")
class CheckoutConcorrenciaIT extends CheckoutConcorrenciaTestBase {
}
