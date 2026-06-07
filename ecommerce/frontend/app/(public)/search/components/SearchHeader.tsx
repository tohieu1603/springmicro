interface SearchHeaderProps {
  query: string;
  total: number;
}

export function SearchHeader({ query, total }: SearchHeaderProps) {
  return (
    <>
      <h1 className="text-h2-d">
        Kết quả tìm kiếm {query && <span className="text-accent">“{query}”</span>}
      </h1>
      <p className="text-sm text-slate mt-1">{total} sản phẩm</p>
    </>
  );
}
