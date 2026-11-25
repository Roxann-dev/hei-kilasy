package hei.school;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class KilasyApplication {

    public static void main(String[] args) {
        SpringApplication.run(KilasyApplication.class, args);
    }

    /**
     * Sends an SMS to a student. The grading code only knows this interface:
     * changing SMS provider only means changing the bean below.
     */
    public interface SmsSender {
        void send(String phone, String text);
    }

    // SMS provider not chosen yet: simulated in the logs
    @Bean
    public SmsSender smsSender() {
        return (phone, text) -> System.out.println("[SMS] " + phone + " : " + text);
    }
}
