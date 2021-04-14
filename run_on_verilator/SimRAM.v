import "DPI-C" function void sim_ram
(
    input  bit        iREn,
    input  longint    iRAddr,
    input  longint    iRSize,
    output longint    iRdata,
    output bit        iRValid,
    input  bit        dREn,
    input  longint    dRAddr,
    input  longint    dRSize,
    output longint    dRdata,
    output bit        dRValid,
    input  longint    dWAddr,
    input  longint    dWdata,
    input  longint    dWSize,
    input  bit        dWEn,
);


module SimRAM (
    input           clk,
    output          mem_iRead_addressInfo_ready,
    input           mem_iRead_addressInfo_valid,
    input [63:0]    mem_iRead_addressInfo_bits_address,
    input [1:0]     mem_iRead_addressInfo_bits_size,
    input           mem_iRead_data_ready,
    output          mem_iRead_data_valid,
    output [63:0]   mem_iRead_data_bits,

    output          mem_dRead_addressInfo_ready,
    input           mem_dRead_addressInfo_valid,
    input [63:0]    mem_dRead_addressInfo_bits_address,
    input [1:0]     mem_dRead_addressInfo_bits_size,
    input           mem_dRead_data_ready,
    output          mem_dRead_data_valid,
    output  [63:0]  mem_dRead_data_bits,

    output          mem_dWrite_storeInfo_ready,
    input           mem_dWrite_storeInfo_valid,
    input [63:0]    mem_dWrite_storeInfo_bits_addressInfo_address,
    input [1:0]     mem_dWrite_storeInfo_bits_addressInfo_size,
    input [63:0]    mem_dWrite_storeInfo_bits_data
);

    assign mem_iRead_addressInfo_ready = 1'b1;
    assign mem_dRead_addressInfo_ready = 1'b1;
    assign mem_dWrite_storeInfo_ready = 1'b1;

    always @(posedge clk) begin
        sim_ram(mem_iRead_addressInfo_valid,
                mem_iRead_addressInfo_bits_address,
                {62'd0, mem_iRead_addressInfo_bits_size},
                mem_iRead_data_bits,
                mem_iRead_data_valid,
                mem_dRead_addressInfo_valid,
                mem_dRead_addressInfo_bits_address,
                {62'd0, mem_dRead_addressInfo_bits_size},
                mem_dRead_data_bits,
                mem_dRead_data_valid,
                mem_dWrite_storeInfo_bits_addressInfo_address,
                mem_dWrite_storeInfo_bits_data,
                {62'd0, mem_dWrite_storeInfo_bits_addressInfo_size},
                mem_dWrite_storeInfo_valid);
    end

endmodule