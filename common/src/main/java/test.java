import io.github.heisenberguwu.myrocketmq.common.config.ConfigRocksDBStorage;

public class test {
    public static void main(String[] args) throws Exception {
        ConfigRocksDBStorage configRocksDBStorage = new ConfigRocksDBStorage("./test.db");
        configRocksDBStorage.put("a".getBytes(),1,"aaa".getBytes());
        byte[] bytes = configRocksDBStorage.get("a".getBytes());

        String s = String.valueOf(bytes);
        System.out.println(s);
    }
}
