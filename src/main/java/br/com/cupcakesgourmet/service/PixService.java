package br.com.cupcakesgourmet.service;

import br.com.cupcakesgourmet.config.AppProperties;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Gera o "PIX copia e cola" (BR Code, padrão EMV do Banco Central) e o QR Code correspondente.
 * PAGAMENTO SIMULADO: a chave é fictícia e não há integração com PSP (limitação documentada).
 */
@Service
public class PixService {

    private final AppProperties.Pix config;

    public PixService(AppProperties props) {
        this.config = props.pix();
    }

    public String gerarCodigo(Long pedidoId, BigDecimal valor) {
        String contaRecebedor = campo("00", "br.gov.bcb.pix")
                + campo("01", config.chave())
                + campo("02", "Pedido " + pedidoId + " SIMULADO");
        String payload = campo("00", "01")
                + campo("01", "12")
                + campo("26", contaRecebedor)
                + campo("52", "0000")
                + campo("53", "986")
                + campo("54", valor.setScale(2, RoundingMode.HALF_UP).toPlainString())
                + campo("58", "BR")
                + campo("59", limpar(config.nomeRecebedor(), 25))
                + campo("60", limpar(config.cidade(), 15))
                + campo("62", campo("05", "CUPCAKES" + pedidoId))
                + "6304";
        return payload + crc16(payload);
    }

    public byte[] gerarQrCodePng(String codigo, int tamanho) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(codigo, BarcodeFormat.QR_CODE, tamanho, tamanho,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            BufferedImage imagem = new BufferedImage(matriz.getWidth(), matriz.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matriz.getWidth(); x++) {
                for (int y = 0; y < matriz.getHeight(); y++) {
                    imagem.setRGB(x, y, matriz.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(imagem, "png", out);
            return out.toByteArray();
        } catch (WriterException e) {
            throw new IllegalStateException("Falha ao gerar QR Code", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** CRC16-CCITT-FALSE (polinômio 0x1021, valor inicial 0xFFFF), exigido pelo BR Code. */
    static String crc16(String dados) {
        int crc = 0xFFFF;
        for (byte b : dados.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
            }
        }
        return String.format("%04X", crc & 0xFFFF);
    }

    private static String campo(String id, String valor) {
        return id + String.format(Locale.ROOT, "%02d", valor.length()) + valor;
    }

    private static String limpar(String texto, int max) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String limpo = semAcento.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]", "");
        return limpo.length() > max ? limpo.substring(0, max) : limpo;
    }
}
