package org.example.audio_ecommerce.LangChain4J;

import org.springframework.stereotype.Component;

@Component
public class SchemaLoader {

    public String loadSchema() {
        return """
                ============================
                TABLE: products
                (Product attributes only)
                ============================
                Columns:
                  product_id (PK),
                  name,
                  brand_name,
                  model,
                  description,
                  short_description,
                  video_url,
                  sku,
                  weight,
                  dimensions,
                  amplifier_type,
                  auto_return,
                  balanced_output,
                  battery_capacity,
                  bit_depth,
                  built_in_effects,
                  channel_count,
                  color,
                  compatible_devices,
                  connection_type,
                  coverage_pattern,
                  crossover_frequency,
                  dac_chipset,
                  driver_configuration,
                  driver_size,
                  enclosure_type,
                  eq_bands,
                  fader_type,
                  frequency_response,
                  has_built_in_battery,
                  has_phantom_power,
                  headphone_accessory_type,
                  headphone_connection_type,
                  headphone_features,
                  headphone_type,
                  impedance,
                  input_channels,
                  input_interface,
                  maxspl,
                  mic_output_impedance,
                  mic_sensitivity,
                  mic_type,
                  midi_support,
                  motor_type,
                  output_channels,
                  output_interface,
                  placement_type,
                  platter_material,
                  plug_type,
                  polar_pattern,
                  power_handling,
                  sample_rate,
                  sensitivity,
                  sirim_approved,
                  sirim_certified,
                  snr,
                  support_airplay,
                  support_bluetooth,
                  support_wifi,
                  thd,
                  tonearm_type,
                  total_power_output,
                  usb_audio_interface,
                  voltage_input,
                  price,
                  discount_price,
                  final_price,
                  category_id (FK → categories.category_id)

                ============================
                TABLE: product_variants
                ============================
                Columns:
                  id (PK),
                  product_id (FK),
                  option_name,
                  option_value,
                  variant_price,
                  variant_stock,
                  variant_sku

                ============================
                TABLE: categories
                ============================
                Columns:
                  category_id (PK),
                  name,
                  description,
                  icon_url,
                  slug,
                  sort_order
                """;
    }
}
