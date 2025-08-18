package com.springboot.kncokbe;

public class LowestPriceDto {
    private String address;
    private Long saleLowestWon;
    private Long jeonseLowestWon;
    private Long wolseDepositLowestWon;
    private Long wolseMonthlyLowestWon;
    private String sourceUrl;

    public LowestPriceDto() {}

    public LowestPriceDto(String address, Long saleLowestWon, Long jeonseLowestWon,
                          Long wolseDepositLowestWon, Long wolseMonthlyLowestWon, String sourceUrl) {
        this.address = address;
        this.saleLowestWon = saleLowestWon;
        this.jeonseLowestWon = jeonseLowestWon;
        this.wolseDepositLowestWon = wolseDepositLowestWon;
        this.wolseMonthlyLowestWon = wolseMonthlyLowestWon;
        this.sourceUrl = sourceUrl;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Long getSaleLowestWon() { return saleLowestWon; }
    public void setSaleLowestWon(Long saleLowestWon) { this.saleLowestWon = saleLowestWon; }

    public Long getJeonseLowestWon() { return jeonseLowestWon; }
    public void setJeonseLowestWon(Long jeonseLowestWon) { this.jeonseLowestWon = jeonseLowestWon; }

    public Long getWolseDepositLowestWon() { return wolseDepositLowestWon; }
    public void setWolseDepositLowestWon(Long wolseDepositLowestWon) { this.wolseDepositLowestWon = wolseDepositLowestWon; }

    public Long getWolseMonthlyLowestWon() { return wolseMonthlyLowestWon; }
    public void setWolseMonthlyLowestWon(Long wolseMonthlyLowestWon) { this.wolseMonthlyLowestWon = wolseMonthlyLowestWon; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
}