package lol.maki.jaguchi.selfservice;

import java.util.List;
import java.util.Set;

import am.ik.yavi.core.ApplicativeValidator;
import am.ik.yavi.core.ConstraintViolations;
import am.ik.yavi.core.Validated;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

	@Test
	void invalidName() {
		final User user = new User("foo*", "foo@email.com", List.of("admin"));
		final ApplicativeValidator<User> validator = User.validator("test", s -> true, () -> Set.of(), Set.of("admin"));
		final Validated<User> userValidated = validator.validate(user);
		assertThat(userValidated.isValid()).isFalse();
		final ConstraintViolations violations = userValidated.errors();
		assertThat(violations).hasSize(1);
		assertThat(violations.get(0).message()).isEqualTo("\"name\" must match [0-9\\-a-zA-Z]*");
	}

	@Test
	void blankName() {
		final User user = new User("", "foo@email.com", List.of("admin"));
		final ApplicativeValidator<User> validator = User.validator("test", s -> true, () -> Set.of(), Set.of("admin"));
		final Validated<User> userValidated = validator.validate(user);
		assertThat(userValidated.isValid()).isFalse();
		final ConstraintViolations violations = userValidated.errors();
		assertThat(violations).hasSize(1);
		assertThat(violations.get(0).message()).isEqualTo("\"name\" must not be blank");
	}

	@Test
	void existingName() {
		final User user = new User("foo", "foo@email.com", List.of("admin"));
		final ApplicativeValidator<User> validator = User.validator("test", s -> true, () -> Set.of("foo"), Set.of("admin"));
		final Validated<User> userValidated = validator.validate(user);
		assertThat(userValidated.isValid()).isFalse();
		final ConstraintViolations violations = userValidated.errors();
		assertThat(violations).hasSize(1);
		assertThat(violations.get(0).message()).isEqualTo("\"name\" must be a new member. \"foo\" is already created.");
	}

	@Test
	void notTeamMember() {
		final User user = new User("foo", "foo@email.com", List.of("admin"));
		final ApplicativeValidator<User> validator = User.validator("test", s -> false, () -> Set.of(), Set.of("admin"));
		final Validated<User> userValidated = validator.validate(user);
		assertThat(userValidated.isValid()).isFalse();
		final ConstraintViolations violations = userValidated.errors();
		assertThat(violations).hasSize(1);
		assertThat(violations.get(0).message()).isEqualTo("\"name\" must be a team member of test. \"foo\" is not a team member.");
	}

	@Test
	void invalidEmail() {
		final User user = new User("foo", "fooemail.com", List.of("admin"));
		final ApplicativeValidator<User> validator = User.validator("test", s -> true, () -> Set.of(), Set.of("admin"));
		final Validated<User> userValidated = validator.validate(user);
		assertThat(userValidated.isValid()).isFalse();
		final ConstraintViolations violations = userValidated.errors();
		assertThat(violations).hasSize(1);
		assertThat(violations.get(0).message()).isEqualTo("\"email\" must be a valid email address");
	}

	@Test
	void blankEmail() {
		final User user = new User("foo", "", List.of("admin"));
		final ApplicativeValidator<User> validator = User.validator("test", s -> true, () -> Set.of(), Set.of("admin"));
		final Validated<User> userValidated = validator.validate(user);
		assertThat(userValidated.isValid()).isFalse();
		final ConstraintViolations violations = userValidated.errors();
		assertThat(violations).hasSize(1);
		assertThat(violations.get(0).message()).isEqualTo("\"email\" must not be blank");
	}

	@Test
	void emptyClusterRoles() {
		final User user = new User("foo", "foo@example.com", List.of());
		final ApplicativeValidator<User> validator = User.validator("test", s -> true, () -> Set.of(), Set.of("admin"));
		final Validated<User> userValidated = validator.validate(user);
		assertThat(userValidated.isValid()).isFalse();
		final ConstraintViolations violations = userValidated.errors();
		assertThat(violations).hasSize(1);
		assertThat(violations.get(0).message()).isEqualTo("\"clusterroles\" must not be empty");
	}

	@Test
	void notAllowedClusterRoles() {
		final User user = new User("foo", "foo@example.com", List.of("admin", "cluster", "admin"));
		final ApplicativeValidator<User> validator = User.validator("test", s -> true, () -> Set.of(), Set.of("admin"));
		final Validated<User> userValidated = validator.validate(user);
		assertThat(userValidated.isValid()).isFalse();
		final ConstraintViolations violations = userValidated.errors();
		assertThat(violations).hasSize(1);
		assertThat(violations.get(0).message()).isEqualTo("Allowed values for \"clusterroles\" are [admin]");
	}
}