package org.example.audio_ecommerce.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ThongSoKyThuatDto {
    private DaiTanSoDto daiTanSo;
    private String congSuat;        // "100W"
    private String troKhang;        // "8Ω"
    private String doNhay;          // "90 dB/W/m"
    private String doMeoTieng;      // "0.5%"
    private String tanSoCrossover;  // "2000 Hz"

    @Getter
    @Setter
    public static class DaiTanSoDto {
        private String tanSoThap; // "50 Hz"
        private String tanSoCao;  // "20000 Hz"
    }
}
