import java.util.function.Consumer;
import java.util.stream.Stream;

class GitHubIssue2 {
    void lambdaHell() {
        stream().flatMap(string -> {
            Consumer consumer = (foo) -> {
            };
            return stream().map(list -> new EvilClass("" , consumer));
        });
    }

    <T> Stream<T> stream() {
        return null;
    }

    static class EvilClass {
        EvilClass(String id, Consumer consumer) {
        }
    }
}
