package br.com.empresa.suporte.web;
import org.springframework.data.domain.*;
import java.util.Set;
public final class Paging {
    private Paging() {}
    public static Pageable page(int page,int size,String sort,Set<String> fields) {
        if (page<0 || page>100000 || size<1 || size>100) throw ApiException.invalid("Página inválida; tamanho deve estar entre 1 e 100.");
        String[] parts=sort.split(",",-1);
        if (parts.length!=2 || !fields.contains(parts[0]) || !(parts[1].equals("asc") || parts[1].equals("desc"))) throw ApiException.invalid("Ordenação inválida.");
        Sort order=Sort.by(parts[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,parts[0]);
        if (!parts[0].equals("id")) order=order.and(Sort.by(Sort.Direction.ASC,"id"));
        return PageRequest.of(page,size,order);
    }
    public static Pageable named(int page,int size) { return page(page,size,"nome,asc",Set.of("nome")); }
    public static Pageable timeline(int page,int size) { return page(page,size,"criadoEm,asc",Set.of("criadoEm")); }
}
