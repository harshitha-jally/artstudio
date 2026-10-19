package com.artstudio.artstudio.config;

import com.artstudio.artstudio.entity.Glaze;
import com.artstudio.artstudio.entity.Painting;
import com.artstudio.artstudio.entity.PotteryItem;
import com.artstudio.artstudio.entity.User;
import com.artstudio.artstudio.repository.GlazeRepository;
import com.artstudio.artstudio.repository.PaintingRepository;
import com.artstudio.artstudio.repository.PotteryItemRepository;
import com.artstudio.artstudio.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;

@Component
public class SeedLoader implements CommandLineRunner {

    private final PaintingRepository paintingRepository;
    private final PotteryItemRepository potteryItemRepository;
    private final GlazeRepository glazeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedLoader(PaintingRepository paintingRepository,
                      PotteryItemRepository potteryItemRepository,
                      GlazeRepository glazeRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.paintingRepository = paintingRepository;
        this.potteryItemRepository = potteryItemRepository;
        this.glazeRepository = glazeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try (InputStream in = new ClassPathResource("seed.json").getInputStream()) {
            root = mapper.readTree(in);
        }

        // Paintings
        if (paintingRepository.count() == 0) {
            for (JsonNode p : root.get("paintings")) {
                Painting painting = new Painting();
                painting.setId(p.get("id").asText());
                painting.setTitle(p.get("title").asText());
                painting.setArtist(p.get("artist").asText());
                painting.setDescription(p.get("description").asText());
                painting.setPrice(new BigDecimal(p.get("price").asText()));
                painting.setImageUrl(p.get("imageUrl").asText());
                painting.setStatus(p.get("status").asText());
                paintingRepository.save(painting);
            }
            System.out.println("Seed: paintings loaded.");
        }

        // Pottery items
        if (potteryItemRepository.count() == 0) {
            for (JsonNode i : root.get("potteryItems")) {
                PotteryItem item = new PotteryItem();
                item.setId(i.get("id").asText());
                item.setName(i.get("name").asText());
                potteryItemRepository.save(item);
            }
            System.out.println("Seed: pottery items loaded.");
        }

        // Glazes
        if (glazeRepository.count() == 0) {
            for (JsonNode g : root.get("glazes")) {
                Glaze glaze = new Glaze();
                glaze.setId(g.get("id").asText());
                glaze.setName(g.get("name").asText());
                glazeRepository.save(glaze);
            }
            System.out.println("Seed: glazes loaded.");
        }

        // Studio account (password is hashed, never stored as plain text)
        JsonNode studio = root.get("studioAccount");
        String studioEmail = studio.get("email").asText();
        if (!userRepository.existsByEmail(studioEmail)) {
            User user = new User();
            user.setName("Studio");
            user.setEmail(studioEmail);
            user.setPasswordHash(passwordEncoder.encode(studio.get("password").asText()));
            user.setRole(studio.get("role").asText());
            userRepository.save(user);
            System.out.println("Seed: studio account created.");
        }
    }
}
