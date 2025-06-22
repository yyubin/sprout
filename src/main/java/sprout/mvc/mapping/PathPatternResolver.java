package sprout.mvc.mapping;

import sprout.beans.annotation.Component;

@Component
public class PathPatternResolver {

    public PathPattern resolve(String pathPatternString) {
        return new PathPattern(pathPatternString);
    }

}
