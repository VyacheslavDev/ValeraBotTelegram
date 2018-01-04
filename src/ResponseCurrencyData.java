import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ResponseCurrencyData {

    private List<List<Double>> price;

    public ResponseCurrencyData(List<List<Double>> price) {
        this.price = price;
    }

    public List<List<Double>> getPrice() {
        return price;
    }

    public void setPrice(List<List<Double>> price) {
        this.price = price;
    }
}
