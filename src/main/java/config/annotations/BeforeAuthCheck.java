package config.annotations;

import domain.grade.MemberGrade;
import message.DescriptionMessage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface BeforeAuthCheck {

}
