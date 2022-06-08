package lol.maki.jaguchi.selfservice;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.ApplicativeValidator;
import am.ik.yavi.core.CustomConstraint;
import com.fasterxml.jackson.annotation.JsonProperty;

record User(String name, String email,
			@JsonProperty("clusterroles") List<String> clusterRoles) {
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		User user = (User) o;
		return Objects.equals(name, user.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	public static ApplicativeValidator<User> validator(String teamName, Predicate<String> isTeamMember, Supplier<Set<String>> existingUsernamesSupplier, Set<String> allowedClusterRoles) {
		return ValidatorBuilder.<User>of()
				.constraint(User::name, "name", c -> c
						.notBlank()
						.pattern("[0-9\\-a-zA-Z]*")
						.predicate(isNewMember(existingUsernamesSupplier))
						.predicate(isTeamMember(teamName, isTeamMember)))
				.constraint(User::email, "email", c -> c
						.notBlank()
						.email())
				.constraint(User::clusterRoles, "clusterroles", c -> c
						.notEmpty()
						.predicate(isAllowedClusterRole(allowedClusterRoles)))
				.build()
				.applicative();
	}

	static CustomConstraint<String> isNewMember(Supplier<Set<String>> existingUsernamesSupplier) {
		return new CustomConstraint<>() {

			@Override
			public boolean test(String s) {
				return !existingUsernamesSupplier.get().contains(s);
			}

			@Override
			public String defaultMessageFormat() {
				return "\"{0}\" must be a new member. \"{1}\" is already created.";
			}

			@Override
			public String messageKey() {
				return "isNewMember";
			}
		};
	}

	static CustomConstraint<String> isTeamMember(String teamName, Predicate<String> isTeamMember) {
		return new CustomConstraint<>() {

			@Override
			public boolean test(String s) {
				return isTeamMember.test(s);
			}

			@Override
			public String defaultMessageFormat() {
				return "\"{0}\" must be a team member of " + teamName + ". \"{1}\" is not a team member.";
			}

			@Override
			public String messageKey() {
				return "isTeamMember";
			}
		};
	}


	static CustomConstraint<List<String>> isAllowedClusterRole(Set<String> allowedClusterRoles) {
		return new CustomConstraint<>() {
			@Override
			public boolean test(List<String> strings) {
				for (String string : strings) {
					if (!allowedClusterRoles.contains(string)) {
						return false;
					}
				}
				return true;
			}

			@Override
			public String defaultMessageFormat() {
				return "Allowed values for \"{0}\" are " + allowedClusterRoles;
			}

			@Override
			public String messageKey() {
				return "isAllowedClusterRole";
			}
		};
	}
}
