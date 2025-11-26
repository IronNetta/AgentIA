package org.seba.agentcli;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class AgentCliApplicationTests {

    @MockBean
    private CliAgent cliAgent;

    @Test
    void contextLoads() {
        // Test that the Spring context loads successfully
        // The CliAgent CommandLineRunner is mocked, so it won't run
    }

}
