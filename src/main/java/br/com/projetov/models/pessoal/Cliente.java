package br.com.projetov.models.pessoal;

public record Cliente(String nomeRazaoSocial, String CpfCnpj, String telefone, String endereco) {
    public Cliente {
        if (nomeRazaoSocial == null || nomeRazaoSocial.isBlank()){
            throw  new IllegalArgumentException("Nome do cliente é obrigatório");
        }
    }
}
